local lvgl = require("lvgl")

-- BandDrip Smart Band 10 Pro NFC hardware probe v1.
-- This is intentionally a transport-independent face: first prove that our own
-- Lua/LVGL watch face installs and executes on DeviceType 567. The Android ->
-- watch live bridge will feed the same render(state) function once transport is proven.

local root = lvgl.Object(nil, {
  w = lvgl.HOR_RES(),
  h = lvgl.VER_RES(),
  bg_color = 0x000000,
  bg_opa = lvgl.OPA(100),
  border_width = 0,
  pad_all = 0,
})
root:clear_flag(lvgl.FLAG.SCROLLABLE)
root:add_flag(lvgl.FLAG.EVENT_BUBBLE)
root:add_flag(lvgl.FLAG.CLICKABLE)

local function label(text, size, color, y)
  return lvgl.Label(root, {
    text = text,
    text_color = color,
    text_font = lvgl.Font("MiSans-Regular", size),
    align = { type = lvgl.ALIGN.TOP_MID, y_ofs = y },
  })
end

local clock = label("--:--", 28, 0x8E8E93, 24)
local glucose = label("112", 88, 0xFFFFFF, 92)
local trend = label("↘", 52, 0xFFFFFF, 119)
trend:set({ x = 105 })
glucose:set({ x = -26 })

local delta = label("+6", 31, 0x64D2FF, 218)
delta:set({ x = -88 })
local separator = label("·", 29, 0x6E6E73, 218)
local age = label("3m ago", 31, 0x8E8E93, 218)
age:set({ x = 72 })

local iob = label("IOB 0.250 U", 25, 0xD1D1D6, 282)
local mode = label("FRESH · TAP TO TEST", 15, 0x68E39A, 374)
local footer = label("BANDDRIP · PRO NFC · V1", 13, 0x5E5E63, 432)

local strike = lvgl.Object(root, {
  w = 214,
  h = 5,
  x = 0,
  y = 147,
  radius = 2,
  bg_color = 0xFF3B30,
  bg_opa = lvgl.OPA(0),
  border_width = 0,
  align = lvgl.ALIGN.TOP_MID,
})
strike:clear_flag(lvgl.FLAG.SCROLLABLE)

local states = {
  {
    glucose = "112",
    trend = "↘",
    delta = "+6",
    age = "3m ago",
    iob = "IOB 0.250 U",
    stale = false,
    mode = "FRESH · TAP TO TEST",
    mode_color = 0x68E39A,
  },
  {
    glucose = "112",
    trend = "↘",
    delta = "+6",
    age = "12m ago",
    iob = "IOB 0.250 U",
    stale = true,
    mode = "STALE 12m · TAP TO TEST",
    mode_color = 0xFF9F0A,
  },
  {
    glucose = "--",
    trend = "→",
    delta = "Δ —",
    age = "no data",
    iob = "IOB —",
    stale = true,
    mode = "NO DATA · TAP TO RESET",
    mode_color = 0xFF453A,
  },
}

local stateIndex = 1

local function render(state)
  local primary = state.stale and 0xFF3B30 or 0xFFFFFF
  glucose:set({ text = state.glucose, text_color = primary })
  trend:set({ text = state.trend, text_color = primary })
  delta:set({ text = state.delta })
  age:set({ text = state.age })
  iob:set({ text = state.iob })
  mode:set({ text = state.mode, text_color = state.mode_color })
  strike:set({ bg_opa = state.stale and lvgl.OPA(100) or lvgl.OPA(0) })
end

root:onevent(lvgl.EVENT.CLICKED, function()
  stateIndex = stateIndex + 1
  if stateIndex > #states then stateIndex = 1 end
  render(states[stateIndex])
end)

local timer = lvgl.Timer {
  period = 1000,
  cb = function(t)
    local ok, value = pcall(os.date, "%H:%M")
    if ok and value then clock:set({ text = value }) end
  end
}

pageOnPause = function()
  timer:pause()
end

pageOnResume = function()
  timer:resume()
end

render(states[stateIndex])
