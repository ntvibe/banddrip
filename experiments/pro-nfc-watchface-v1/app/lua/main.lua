local lvgl = require("lvgl")

-- BandDrip Smart Band 10 Pro NFC hardware test face v1.
--
-- IMPORTANT:
--   This first build intentionally renders DEMO data only.
--   Its job is to prove that a 336x480 Lua watchface can be installed and
--   switched to safely on the user's Smart Band 10 Pro NFC.
--
-- The next milestone is replacing readDemoState() with state written by the
-- Android BandDrip relay over a hardware-proven transport.

local W = 336
local H = 480
local STALE_MINUTES = 10

local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_STALE = 0xFF3B30
local COLOR_DELTA = 0x64D2FF
local COLOR_AGE = 0x8E8E93
local COLOR_IOB = 0xD1D1D6
local COLOR_DIM = 0x6E6E73

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

local demoLabel = lvgl.Label(root, {
    x = 0,
    y = 26,
    width = W,
    text = "BANDDRIP  •  DEMO",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 18),
    text_align = lvgl.ALIGN.TOP_MID,
})

local glucose = lvgl.Label(root, {
    x = 22,
    y = 86,
    width = 220,
    text = "112",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 86),
    text_align = lvgl.ALIGN.TOP_MID,
})

local trend = lvgl.Label(root, {
    x = 229,
    y = 110,
    width = 86,
    text = "↘",
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
    text = "+6",
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
    text = "3m ago",
    text_color = COLOR_AGE,
    text_font = lvgl.Font("MiSans-Regular", 31),
    text_align = lvgl.ALIGN.TOP_MID,
})

local iob = lvgl.Label(root, {
    x = 0,
    y = 282,
    width = W,
    text = "IOB 0.250 U",
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

-- v1 deliberately has a deterministic local state. This lets us distinguish
-- "the face installs and runs" from later Android/BLE transport problems.
local state = {
    glucose = 112,
    delta = 6,
    trend = "↘",
    ageMinutes = 3,
    iob = 0.250,
}

local function renderState()
    local stale = state.ageMinutes >= STALE_MINUTES
    local mainColor = stale and COLOR_STALE or COLOR_PRIMARY

    glucose:set {
        text = tostring(state.glucose),
        text_color = mainColor,
    }
    trend:set {
        text = state.trend,
        text_color = mainColor,
    }
    delta:set {
        text = string.format("%+d", state.delta),
    }
    age:set {
        text = tostring(state.ageMinutes) .. "m ago",
    }
    iob:set {
        text = string.format("IOB %.3f U", state.iob),
    }

    if stale then
        strike:set { w = 230 }
    else
        strike:set { w = 0 }
    end
end

local function renderClock()
    clock:set { text = os.date("%H:%M") }
    dateLabel:set { text = string.upper(os.date("%a %d %b")) }
end

renderState()
renderClock()

local timer = lvgl.Timer {
    period = 15000,
    cb = function(_)
        renderClock()
    end,
}

pageOnPause = function()
    timer:pause()
end

pageOnResume = function()
    renderState()
    renderClock()
    timer:resume()
end
