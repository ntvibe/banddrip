local lvgl = require("lvgl")

-- BandDrip Smart Band 10 Pro NFC watchface v2.
-- v2 keeps the hardware-proven DeviceType 367 / 336x480 Lua container from v1
-- and replaces static demo data with read-only discovery of known glucose state
-- files used by WatchDrip-compatible Quick Apps and BandDrip's own glance bridge.

local W = 336
local H = 480
local STALE_MINUTES = 10
local REFRESH_MS = 15000

local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_STALE = 0xFF3B30
local COLOR_DELTA = 0x64D2FF
local COLOR_AGE = 0x8E8E93
local COLOR_IOB = 0xD1D1D6
local COLOR_DIM = 0x6E6E73
local COLOR_SOURCE = 0x30D158

local SOURCES = {
    -- Community WatchDrip-compatible paths proven on Xiaomi Band 9 Lua faces.
    { kind = "watchdrip", path = "//data/quickapp/files/com.thatguysservice.huami_xdrip/info.json", label = "WATCHDRIP" },
    { kind = "watchdrip", path = "//data/quickapp/files/com.application.watch.watchdrip/info.json", label = "WATCHDRIP" },

    -- BandDrip glance bridge candidates. Keep these read-only until hardware proves
    -- the exact Smart Band 10 Pro NFC Quick App sandbox mapping.
    { kind = "banddrip", path = "//data/quickapp/files/org.banddrip.app/glance.json", label = "BANDDRIP" },
    { kind = "banddrip", path = "/data/quickapp/file/org.banddrip.app/glance.json", label = "BANDDRIP" },
    { kind = "banddrip", path = "/data/quickapp/files/org.banddrip.app/glance.json", label = "BANDDRIP" },
    { kind = "banddrip", path = "/data/quickapp/file/org.banddrip.app/files/glance.json", label = "BANDDRIP" },
}

local rootbase = lvgl.Object(nil, {
    w = lvgl.HOR_RES(),
    h = lvgl.VER_RES(),
    bg_color = COLOR_BG,
    bg_opa = lvgl.OPA(100),
    border_width = 0,
})
rootbase:clear_flag(lvgl.FLAG.SCROLLABLE)
rootbase:add_flag(lvgl.FLAG.EVENT_BUBBLE)

local root = lvgl.Object(rootbase, {
    w = W,
    h = H,
    align = lvgl.ALIGN.CENTER,
    bg_color = COLOR_BG,
    bg_opa = lvgl.OPA(100),
    outline_width = 0,
    border_width = 0,
    pad_all = 0,
})
root:clear_flag(lvgl.FLAG.SCROLLABLE)
root:add_flag(lvgl.FLAG.EVENT_BUBBLE)

local sourceLabel = lvgl.Label(root, {
    x = 0,
    y = 24,
    width = W,
    text = "BANDDRIP V2  •  WAITING",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 17),
    text_align = lvgl.ALIGN.TOP_MID,
})

local glucose = lvgl.Label(root, {
    x = 20,
    y = 82,
    width = 222,
    text = "---",
    text_color = COLOR_STALE,
    text_font = lvgl.Font("MiSans-Regular", 86),
    text_align = lvgl.ALIGN.TOP_MID,
})

local trend = lvgl.Label(root, {
    x = 228,
    y = 108,
    width = 88,
    text = "",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 52),
    text_align = lvgl.ALIGN.TOP_MID,
})

local strike = lvgl.Object(root, {
    x = 44,
    y = 145,
    w = 0,
    h = 5,
    bg_color = COLOR_STALE,
    bg_opa = lvgl.OPA(100),
    border_width = 0,
    radius = 2,
})

local delta = lvgl.Label(root, {
    x = 24,
    y = 217,
    width = 100,
    text = "--",
    text_color = COLOR_DELTA,
    text_font = lvgl.Font("MiSans-Regular", 31),
    text_align = lvgl.ALIGN.TOP_MID,
})

local separator = lvgl.Label(root, {
    x = 128,
    y = 217,
    width = 32,
    text = "·",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 31),
    text_align = lvgl.ALIGN.TOP_MID,
})

local age = lvgl.Label(root, {
    x = 158,
    y = 217,
    width = 150,
    text = "--m ago",
    text_color = COLOR_AGE,
    text_font = lvgl.Font("MiSans-Regular", 31),
    text_align = lvgl.ALIGN.TOP_MID,
})

local iob = lvgl.Label(root, {
    x = 0,
    y = 282,
    width = W,
    text = "IOB —",
    text_color = COLOR_IOB,
    text_font = lvgl.Font("MiSans-Regular", 27),
    text_align = lvgl.ALIGN.TOP_MID,
})

local clock = lvgl.Label(root, {
    x = 0,
    y = 365,
    width = W,
    text = "--:--",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 44),
    text_align = lvgl.ALIGN.TOP_MID,
})

local dateLabel = lvgl.Label(root, {
    x = 0,
    y = 417,
    width = W,
    text = "",
    text_color = COLOR_AGE,
    text_font = lvgl.Font("MiSans-Regular", 18),
    text_align = lvgl.ALIGN.TOP_MID,
})

local activeSourcePath = nil
local activeSourceKind = nil
local activeSourceLabel = nil
local lastJson = nil
local state = nil

local function readText(path)
    local file = io.open(path, "r")
    if not file then return nil end
    local ok, content = pcall(function() return file:read("*all") end)
    file:close()
    if not ok then return nil end
    if not content or #content == 0 then return nil end
    return content
end

local function findReadableSource()
    if activeSourcePath then
        local content = readText(activeSourcePath)
        if content then
            return content, activeSourceKind, activeSourceLabel, activeSourcePath
        end
        activeSourcePath = nil
        activeSourceKind = nil
        activeSourceLabel = nil
    end

    for _, source in ipairs(SOURCES) do
        local content = readText(source.path)
        if content then
            activeSourcePath = source.path
            activeSourceKind = source.kind
            activeSourceLabel = source.label
            return content, source.kind, source.label, source.path
        end
    end

    return nil, nil, nil, nil
end

local function jsonObject(json, key)
    if not json then return nil end
    return json:match('"' .. key .. '"%s*:%s*(%b{})')
end

local function jsonString(json, key)
    if not json then return nil end
    return json:match('"' .. key .. '"%s*:%s*"([^"]*)"')
end

local function jsonNumber(json, key)
    if not json then return nil end
    local raw = json:match('"' .. key .. '"%s*:%s*"([%+%-0-9%.]+)"')
    if not raw then
        raw = json:match('"' .. key .. '"%s*:%s*([%+%-0-9%.]+)')
    end
    if not raw then return nil end
    return tonumber(raw)
end

local function jsonBoolean(json, key, fallback)
    if not json then return fallback end
    if json:match('"' .. key .. '"%s*:%s*true') then return true end
    if json:match('"' .. key .. '"%s*:%s*false') then return false end
    return fallback
end

local function normalizeTimestampMs(value)
    if not value or value <= 0 then return nil end
    if value < 100000000000 then return value * 1000 end
    return value
end

local function parseBandDrip(json)
    local glucoseValue = jsonNumber(json, "glucose")
    local timestamp = normalizeTimestampMs(jsonNumber(json, "glucoseTimestampMs") or jsonNumber(json, "timestampMs"))
    if not glucoseValue or not timestamp then return nil end

    return {
        glucose = glucoseValue,
        delta = jsonNumber(json, "delta"),
        trend = jsonString(json, "trend") or "None",
        timestampMs = timestamp,
        units = jsonString(json, "units") or "mg/dL",
        iob = jsonNumber(json, "iobUnits"),
        iobTimestampMs = normalizeTimestampMs(jsonNumber(json, "iobTimestampMs")),
        showIob = jsonBoolean(json, "showIob", true),
        source = "BANDDRIP",
    }
end

local function parseWatchDrip(json)
    local bg = jsonObject(json, "bg")
    if not bg then return nil end

    local status = jsonObject(json, "status") or ""
    local glucoseValue = jsonNumber(bg, "val")
    local timestamp = normalizeTimestampMs(jsonNumber(bg, "time"))
    if not glucoseValue or not timestamp then return nil end

    local isMgdl = jsonBoolean(status, "isMgdl", true)

    return {
        glucose = glucoseValue,
        delta = jsonNumber(bg, "delta"),
        trend = jsonString(bg, "trend") or "None",
        timestampMs = timestamp,
        units = isMgdl and "mg/dL" or "mmol/L",
        iob = nil,
        iobTimestampMs = nil,
        showIob = true,
        source = "WATCHDRIP",
    }
end

local function parseState(json, kind)
    if kind == "watchdrip" then return parseWatchDrip(json) end
    if kind == "banddrip" then return parseBandDrip(json) end
    return nil
end

local function nowMs()
    return os.time() * 1000
end

local function ageMinutes(timestampMs)
    if not timestampMs or timestampMs <= 0 then return nil end
    local value = math.floor((nowMs() - timestampMs) / 60000)
    if value < 0 then value = 0 end
    return value
end

local function normalizeTrend(value)
    if not value then return "" end
    return string.lower((value:gsub("[%s_%-]", "")))
end

local function trendArrow(value)
    local v = normalizeTrend(value)
    if v == "doubleup" then return "⇈" end
    if v == "singleup" then return "↑" end
    if v == "fortyfiveup" then return "↗" end
    if v == "flat" then return "→" end
    if v == "fortyfivedown" then return "↘" end
    if v == "singledown" then return "↓" end
    if v == "doubledown" then return "⇊" end
    return ""
end

local function roundNearest(value)
    if value >= 0 then return math.floor(value + 0.5) end
    return math.ceil(value - 0.5)
end

local function glucoseText(s)
    if s.units == "mmol/L" then return string.format("%.1f", s.glucose) end
    return string.format("%d", roundNearest(s.glucose))
end

local function deltaText(s)
    if s.delta == nil then return "--" end
    if s.units == "mmol/L" then return string.format("%+.1f", s.delta) end
    return string.format("%+d", roundNearest(s.delta))
end

local function setStrike(stale, textLength)
    if not stale then
        strike:set { w = 0 }
        return
    end
    local width = math.min(230, math.max(100, textLength * 58))
    strike:set { x = math.floor((W - width) / 2) - 22, w = width }
end

local function clearReading()
    sourceLabel:set { text = "BANDDRIP V2  •  WAITING", text_color = COLOR_DIM }
    glucose:set { text = "---", text_color = COLOR_STALE }
    trend:set { text = "", text_color = COLOR_STALE }
    delta:set { text = "--" }
    age:set { text = "--m ago" }
    iob:set { text = "IOB —" }
    strike:set { w = 0 }
end

local function renderReading(s)
    local glucoseAge = ageMinutes(s.timestampMs)
    local stale = glucoseAge == nil or glucoseAge >= STALE_MINUTES
    local mainColor = stale and COLOR_STALE or COLOR_PRIMARY
    local gText = glucoseText(s)

    sourceLabel:set {
        text = "BANDDRIP V2  •  " .. (s.source or "LIVE"),
        text_color = COLOR_SOURCE,
    }
    glucose:set { text = gText, text_color = mainColor }
    trend:set { text = trendArrow(s.trend), text_color = mainColor }
    delta:set { text = deltaText(s) }
    age:set { text = glucoseAge and (tostring(glucoseAge) .. "m ago") or "--m ago" }
    setStrike(stale, #gText)

    if s.showIob == false then
        iob:set { text = "" }
    elseif s.iob ~= nil then
        local iobAge = ageMinutes(s.iobTimestampMs or s.timestampMs)
        if iobAge ~= nil and iobAge < STALE_MINUTES then
            iob:set { text = string.format("IOB %.3f U", s.iob) }
        else
            iob:set { text = "IOB —" }
        end
    else
        iob:set { text = "IOB —" }
    end
end

local function renderClock()
    clock:set { text = os.date("%H:%M") }
    dateLabel:set { text = string.upper(os.date("%a %d %b")) }
end

local function render()
    renderClock()

    local json, kind = findReadableSource()
    if json then
        if json ~= lastJson or not state then
            local parsed = parseState(json, kind)
            if parsed then state = parsed end
            lastJson = json
        end
    else
        state = nil
        lastJson = nil
    end

    if state then
        renderReading(state)
    else
        clearReading()
    end
end

render()

local timer = lvgl.Timer {
    period = REFRESH_MS,
    cb = function(_) render() end,
}

pageOnPause = function()
    timer:pause()
end

pageOnResume = function()
    render()
    timer:resume()
end
