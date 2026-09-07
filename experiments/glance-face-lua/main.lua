local lvgl = require("lvgl")

-- BandDrip glance-face experiment for Xiaomi Band 10-class Vela/NuttX devices.
--
-- Hypothesis under test:
--   Vela RPK writes internal://files/glance.json
--   -> underlying NuttX path is /data/quickapp/file/org.banddrip.app/glance.json
--   -> Lua watch-face runtime can read that file directly.
--
-- This is intentionally a read-only probe. It never mutates Quick App storage.

local CANDIDATE_PATHS = {
  "/data/quickapp/file/org.banddrip.app/glance.json",
  "/data/quickapp/files/org.banddrip.app/glance.json",
  "/data/quickapp/file/org.banddrip.app/files/glance.json",
}

local STALE_MINUTES = 10
local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_STALE = 0xFF3B30
local COLOR_META = 0x64D2FF
local COLOR_AGE = 0x8E8E93
local COLOR_IOB = 0xD1D1D6
local COLOR_TIME = 0xFFFFFF
local COLOR_DATE = 0x8E8E93

local root = lvgl.Object(nil, {
  w = lvgl.HOR_RES(),
  h = lvgl.VER_RES(),
  bg_color = COLOR_BG,
  border_width = 0,
  pad_all = 0,
})
root:clear_flag(lvgl.FLAG.SCROLLABLE)
root:add_flag(lvgl.FLAG.EVENT_BUBBLE)

local glucose = lvgl.Label(root, {
  text = "—",
  text_color = COLOR_PRIMARY,
  text_font = lvgl.Font("MiSans-Regular", 76),
  align = { type = lvgl.ALIGN.TOP_MID, y_ofs = 92 },
})

local trend = lvgl.Label(root, {
  text = "",
  text_color = COLOR_PRIMARY,
  text_font = lvgl.Font("MiSans-Regular", 46),
})

local strike = lvgl.Object(root, {
  w = 0,
  h = 4,
  bg_color = COLOR_STALE,
  border_width = 0,
  radius = 2,
})

local meta = lvgl.Label(root, {
  text = "Δ —  ·  age —",
  text_color = COLOR_META,
  text_font = lvgl.Font("MiSans-Regular", 29),
  align = { type = lvgl.ALIGN.TOP_MID, y_ofs = 220 },
})

local iob = lvgl.Label(root, {
  text = "IOB —",
  text_color = COLOR_IOB,
  text_font = lvgl.Font("MiSans-Regular", 25),
  align = { type = lvgl.ALIGN.TOP_MID, y_ofs = 286 },
})

local clock = lvgl.Label(root, {
  text = "--:--",
  text_color = COLOR_TIME,
  text_font = lvgl.Font("MiSans-Regular", 42),
  align = { type = lvgl.ALIGN.BOTTOM_MID, y_ofs = -78 },
})

local dateLabel = lvgl.Label(root, {
  text = "",
  text_color = COLOR_DATE,
  text_font = lvgl.Font("MiSans-Regular", 20),
  align = { type = lvgl.ALIGN.BOTTOM_MID, y_ofs = -44 },
})

local sourcePath = nil
local lastJson = nil
local state = nil

local function readText(path)
  local f = io.open(path, "r")
  if not f then return nil end
  local text = f:read("*a")
  f:close()
  return text
end

local function findStateFile()
  if sourcePath then
    local value = readText(sourcePath)
    if value then return value end
    sourcePath = nil
  end

  for _, path in ipairs(CANDIDATE_PATHS) do
    local value = readText(path)
    if value then
      sourcePath = path
      return value
    end
  end
  return nil
end

local function jsonNumber(json, key)
  local raw = json:match('"' .. key .. '"%s*:%s*([%-0-9%.]+)')
  if not raw then return nil end
  return tonumber(raw)
end

local function jsonString(json, key)
  return json:match('"' .. key .. '"%s*:%s*"([^"]*)"')
end

local function jsonBoolean(json, key, fallback)
  local raw = json:match('"' .. key .. '"%s*:%s*(true)')
  if raw then return true end
  raw = json:match('"' .. key .. '"%s*:%s*(false)')
  if raw then return false end
  return fallback
end

local function parseState(json)
  if not json or #json == 0 then return nil end
  local parsed = {
    glucose = jsonNumber(json, "glucose"),
    units = jsonString(json, "units") or "mg/dL",
    delta = jsonNumber(json, "delta"),
    trend = jsonString(json, "trend") or "unknown",
    glucoseTimestampMs = jsonNumber(json, "glucoseTimestampMs"),
    iobUnits = jsonNumber(json, "iobUnits"),
    iobTimestampMs = jsonNumber(json, "iobTimestampMs"),
    showIob = jsonBoolean(json, "showIob", true),
  }
  if not parsed.glucose or not parsed.glucoseTimestampMs then return nil end
  return parsed
end

local function nowMs()
  return os.time() * 1000
end

local function ageMinutes(timestampMs)
  if not timestampMs or timestampMs <= 0 then return nil end
  local age = math.floor((nowMs() - timestampMs) / 60000)
  if age < 0 then age = 0 end
  return age
end

local function trendArrow(value)
  if value == "doubleUp" then return "⇈" end
  if value == "singleUp" then return "↑" end
  if value == "fortyFiveUp" then return "↗" end
  if value == "flat" then return "→" end
  if value == "fortyFiveDown" then return "↘" end
  if value == "singleDown" then return "↓" end
  if value == "doubleDown" then return "⇊" end
  return "?"
end

local function glucoseText(s)
  if s.units == "mmol/L" then return string.format("%.1f", s.glucose) end
  return string.format("%d", math.floor(s.glucose + 0.5))
end

local function deltaText(s)
  if s.delta == nil then return "Δ —" end
  if s.units == "mmol/L" then return string.format("%+.1f", s.delta) end
  return string.format("%+d", math.floor(s.delta + (s.delta >= 0 and 0.5 or -0.5)))
end

local function setStrike(stale, glucoseChars)
  if not stale then
    strike:set { w = 0 }
    return
  end

  -- Conservative width estimate; hardware pass will replace this with measured
  -- glyph width if the Band 10 Lua binding exposes label geometry reliably.
  local width = math.min(150, math.max(72, glucoseChars * 42))
  strike:set { w = width, x = math.floor((lvgl.HOR_RES() - width) / 2) - 12, y = 137 }
end

local function render()
  clock:set { text = os.date("%H:%M") }
  dateLabel:set { text = string.upper(os.date("%a %d")) }

  local json = findStateFile()
  if json and json ~= lastJson then
    local parsed = parseState(json)
    if parsed then state = parsed end
    lastJson = json
  end

  if not state then
    glucose:set { text = "—", text_color = COLOR_STALE }
    trend:set { text = "" }
    meta:set { text = "Δ —  ·  age —" }
    iob:set { text = "IOB —" }
    setStrike(false, 1)
    return
  end

  local age = ageMinutes(state.glucoseTimestampMs)
  local stale = age == nil or age >= STALE_MINUTES
  local mainColor = stale and COLOR_STALE or COLOR_PRIMARY
  local gText = glucoseText(state)

  glucose:set { text = gText, text_color = mainColor }
  trend:set {
    text = trendArrow(state.trend),
    text_color = mainColor,
    x = math.floor(lvgl.HOR_RES() / 2) + 52,
    y = 112,
  }
  setStrike(stale, #gText)

  local ageText = age and (tostring(age) .. "m ago") or "age —"
  meta:set { text = deltaText(state) .. "  ·  " .. ageText }

  if state.showIob == false then
    iob:set { text = "" }
  else
    local iobAge = ageMinutes(state.iobTimestampMs)
    if state.iobUnits ~= nil and iobAge ~= nil and iobAge < STALE_MINUTES then
      iob:set { text = string.format("IOB %.3f U", state.iobUnits) }
    else
      iob:set { text = "IOB —" }
    end
  end
end

render()

local refreshTimer = lvgl.Timer {
  period = 15000,
  cb = function(_)
    render()
  end,
}

pageOnPause = function()
  refreshTimer:pause()
end

pageOnResume = function()
  render()
  refreshTimer:resume()
end
