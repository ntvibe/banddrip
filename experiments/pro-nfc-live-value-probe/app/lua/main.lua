local lvgl = require("lvgl")
local dataman = require("dataman")

-- BandDrip Smart Band 10 Pro NFC live-value probe.
--
-- Purpose: prove that a stock-firmware Lua watchface can receive a value that
-- Gadgetbridge changes on the phone without reinstalling the .face.
--
-- Transport under test: Xiaomi watchface dataman topics AppAlarmHour and
-- AppAlarmMinute. We encode one integer as base-60:
--
--     encodedValue = alarmHour * 60 + alarmMinute
--
-- Example: 02:03 => 123, 02:04 => 124.
--
-- This is deliberately a protocol probe, not the final BandDrip transport.

local W = 336
local H = 480

local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_OK = 0x30D158
local COLOR_WAIT = 0xFF9F0A
local COLOR_DIM = 0x8E8E93
local COLOR_ACCENT = 0x64D2FF

local alarmHour = nil
local alarmMinute = nil
local battery = nil
local bluetooth = nil

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

local statusLabel = lvgl.Label(root, {
    x = 0,
    y = 28,
    width = W,
    text = "ALARM CHANNEL  •  WAITING",
    text_color = COLOR_WAIT,
    text_font = lvgl.Font("MiSans-Regular", 17),
    text_align = lvgl.ALIGN.TOP_MID,
})

local valueLabel = lvgl.Label(root, {
    x = 0,
    y = 92,
    width = W,
    text = "---",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 92),
    text_align = lvgl.ALIGN.TOP_MID,
})

local arrowLabel = lvgl.Label(root, {
    x = 0,
    y = 190,
    width = W,
    text = "→",
    text_color = COLOR_ACCENT,
    text_font = lvgl.Font("MiSans-Regular", 40),
    text_align = lvgl.ALIGN.TOP_MID,
})

local rawLabel = lvgl.Label(root, {
    x = 0,
    y = 250,
    width = W,
    text = "alarm --:--",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 26),
    text_align = lvgl.ALIGN.TOP_MID,
})

local hintLabel = lvgl.Label(root, {
    x = 18,
    y = 305,
    width = W - 36,
    text = "TEST  02:03 = 123\nTHEN  02:04 = 124",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 18),
    text_align = lvgl.ALIGN.TOP_MID,
})

local healthLabel = lvgl.Label(root, {
    x = 0,
    y = 380,
    width = W,
    text = "DATAMAN  BAT --%  BT --",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 15),
    text_align = lvgl.ALIGN.TOP_MID,
})

local clockLabel = lvgl.Label(root, {
    x = 0,
    y = 426,
    width = W,
    text = "--:--",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 28),
    text_align = lvgl.ALIGN.TOP_MID,
})

local function fixedToInt(value)
    if value == nil then return nil end
    return value // 256
end

local function renderTransport()
    if alarmHour == nil or alarmMinute == nil then
        statusLabel:set { text = "ALARM CHANNEL  •  WAITING", text_color = COLOR_WAIT }
        valueLabel:set { text = "---" }
        rawLabel:set { text = "alarm --:--" }
        return
    end

    local h = fixedToInt(alarmHour)
    local m = fixedToInt(alarmMinute)

    if h == nil or m == nil or h < 0 or h > 23 or m < 0 or m > 59 then
        statusLabel:set { text = "ALARM CHANNEL  •  INVALID", text_color = COLOR_WAIT }
        valueLabel:set { text = "ERR" }
        rawLabel:set { text = string.format("raw %s / %s", tostring(h), tostring(m)) }
        return
    end

    local decoded = h * 60 + m
    statusLabel:set { text = "ALARM CHANNEL  •  LIVE", text_color = COLOR_OK }
    valueLabel:set { text = tostring(decoded) }
    rawLabel:set { text = string.format("alarm %02d:%02d", h, m) }
end

local function renderHealth()
    local b = battery and fixedToInt(battery) or nil
    local bt = bluetooth and fixedToInt(bluetooth) or nil
    local bText = b and (tostring(b) .. "%") or "--%"
    local btText = bt and tostring(bt) or "--"
    healthLabel:set { text = "DATAMAN  BAT " .. bText .. "  BT " .. btText }
end

local function renderClock()
    clockLabel:set { text = os.date("%H:%M:%S") }
end

-- These topic names come from Xiaomi's watchface dataman source table.
-- Values are fixed point, with the integer in value // 256.
dataman.subscribe("AppAlarmHour", root, function(_, value)
    alarmHour = value
    renderTransport()
end)

dataman.subscribe("AppAlarmMinute", root, function(_, value)
    alarmMinute = value
    renderTransport()
end)

-- Control topics: if these move, dataman itself is alive even if the alarm
-- topics are unavailable on this firmware.
dataman.subscribe("systemStatusBattery", root, function(_, value)
    battery = value
    renderHealth()
end)

dataman.subscribe("systemStatusBluetooth", root, function(_, value)
    bluetooth = value
    renderHealth()
end)

renderTransport()
renderHealth()
renderClock()

local clockTimer = lvgl.Timer {
    period = 1000,
    cb = function(_) renderClock() end,
}

pageOnPause = function()
    clockTimer:pause()
end

pageOnResume = function()
    renderTransport()
    renderHealth()
    renderClock()
    clockTimer:resume()
end
