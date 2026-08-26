local lvgl = require("lvgl")
local dataman = require("dataman")

-- BandDrip stock-firmware glucose face for Xiaomi Smart Band 10 Pro NFC.
-- Android encodes glucose state into Gadgetbridge weather fields; this face
-- decodes them and renders a diabetes-first glance display.

local W = 336
local H = 480
local INT_MAX = 2147483647
local STALE_MINUTES = 10
local REFRESH_MS = 15000

local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_STALE = 0xFF3B30
local COLOR_DELTA = 0x64D2FF
local COLOR_AGE = 0xA1A1A6
local COLOR_IOB = 0xD1D1D6
local COLOR_DIM = 0x636366
local COLOR_OK = 0x30D158

local state = {
    glucose = nil,
    age = nil,
    aqi = nil,
    trend = nil,
    pressure = nil,
    lastTransportUpdate = nil,
}

local rootbase = lvgl.Object(nil, {
    w = lvgl.HOR_RES(), h = lvgl.VER_RES(),
    bg_color = COLOR_BG, bg_opa = lvgl.OPA(100), border_width = 0,
})
rootbase:clear_flag(lvgl.FLAG.SCROLLABLE)
rootbase:add_flag(lvgl.FLAG.EVENT_BUBBLE)

local root = lvgl.Object(rootbase, {
    w = W, h = H, align = lvgl.ALIGN.CENTER,
    bg_color = COLOR_BG, bg_opa = lvgl.OPA(100),
    border_width = 0, outline_width = 0, pad_all = 0,
})
root:clear_flag(lvgl.FLAG.SCROLLABLE)
root:add_flag(lvgl.FLAG.EVENT_BUBBLE)

local statusLabel = lvgl.Label(root, {
    x = 0, y = 22, width = W,
    text = "BANDDRIP  •  WAITING",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 16),
    text_align = lvgl.ALIGN.TOP_MID,
})

local glucoseLabel = lvgl.Label(root, {
    x = 18, y = 72, width = 225,
    text = "---",
    text_color = COLOR_STALE,
    text_font = lvgl.Font("MiSans-Regular", 88),
    text_align = lvgl.ALIGN.TOP_MID,
})

local trendLabel = lvgl.Label(root, {
    x = 224, y = 104, width = 96,
    text = "",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 54),
    text_align = lvgl.ALIGN.TOP_MID,
})

local strike = lvgl.Object(root, {
    x = 45, y = 142, w = 0, h = 5,
    bg_color = COLOR_STALE, bg_opa = lvgl.OPA(100),
    border_width = 0, radius = 2,
})

local deltaLabel = lvgl.Label(root, {
    x = 22, y = 207, width = 110,
    text = "--",
    text_color = COLOR_DELTA,
    text_font = lvgl.Font("MiSans-Regular", 32),
    text_align = lvgl.ALIGN.TOP_MID,
})

local dotLabel = lvgl.Label(root, {
    x = 132, y = 207, width = 28,
    text = "·",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 32),
    text_align = lvgl.ALIGN.TOP_MID,
})

local ageLabel = lvgl.Label(root, {
    x = 158, y = 207, width = 154,
    text = "--m ago",
    text_color = COLOR_AGE,
    text_font = lvgl.Font("MiSans-Regular", 30),
    text_align = lvgl.ALIGN.TOP_MID,
})

local iobLabel = lvgl.Label(root, {
    x = 0, y = 276, width = W,
    text = "IOB —",
    text_color = COLOR_IOB,
    text_font = lvgl.Font("MiSans-Regular", 27),
    text_align = lvgl.ALIGN.TOP_MID,
})

local clockLabel = lvgl.Label(root, {
    x = 0, y = 354, width = W,
    text = "--:--",
    text_color = COLOR_PRIMARY,
    text_font = lvgl.Font("MiSans-Regular", 46),
    text_align = lvgl.ALIGN.TOP_MID,
})

local dateLabel = lvgl.Label(root, {
    x = 0, y = 410, width = W,
    text = "",
    text_color = COLOR_AGE,
    text_font = lvgl.Font("MiSans-Regular", 18),
    text_align = lvgl.ALIGN.TOP_MID,
})

local footer = lvgl.Label(root, {
    x = 0, y = 450, width = W,
    text = "GADGETBRIDGE WEATHER LINK",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 10),
    text_align = lvgl.ALIGN.TOP_MID,
})

local function decodeFixed(value)
    if value == nil or type(value) ~= "number" then return nil end
    if value == INT_MAX then return nil end
    return value // 256
end

local function trendArrow(code)
    if code == 1 then return "⇊" end
    if code == 2 then return "↓" end
    if code == 3 then return "↘" end
    if code == 4 then return "→" end
    if code == 5 then return "↗" end
    if code == 6 then return "↑" end
    if code == 7 then return "⇈" end
    return ""
end

local function effectiveAge()
    if state.age == nil then return nil end
    local extra = 0
    if state.lastTransportUpdate ~= nil then
        extra = math.floor(math.max(0, os.time() - state.lastTransportUpdate) / 60)
    end
    return state.age + extra
end

local function decodedDelta()
    if state.aqi == nil then return nil end
    if state.aqi < 1 or state.aqi > 199 then return nil end
    return state.aqi - 100
end

local function decodedIob()
    if state.pressure == nil or state.pressure < 100 then return nil end
    return (state.pressure - 100) / 1000
end

local function setStrike(stale, glucoseText)
    if not stale then
        strike:set { w = 0 }
        return
    end
    local width = math.min(225, math.max(105, #glucoseText * 58))
    strike:set { x = math.floor((W - width) / 2) - 20, w = width }
end

local function renderClock()
    clockLabel:set { text = os.date("%H:%M") }
    dateLabel:set { text = string.upper(os.date("%a %d %b")) }
end

local function renderNoData()
    statusLabel:set { text = "BANDDRIP  •  NO DATA", text_color = COLOR_STALE }
    glucoseLabel:set { text = "---", text_color = COLOR_STALE }
    trendLabel:set { text = "", text_color = COLOR_STALE }
    deltaLabel:set { text = "--" }
    ageLabel:set { text = "--m ago", text_color = COLOR_STALE }
    iobLabel:set { text = "IOB —" }
    strike:set { w = 0 }
end

local function render()
    renderClock()

    if state.glucose == nil or state.glucose <= 0 then
        renderNoData()
        return
    end

    local age = effectiveAge()
    local stale = age == nil or age >= STALE_MINUTES
    local mainColor = stale and COLOR_STALE or COLOR_PRIMARY
    local glucoseText = tostring(state.glucose)

    statusLabel:set {
        text = stale and "BANDDRIP  •  STALE" or "BANDDRIP  •  LIVE",
        text_color = stale and COLOR_STALE or COLOR_OK,
    }
    glucoseLabel:set { text = glucoseText, text_color = mainColor }
    trendLabel:set { text = trendArrow(state.trend), text_color = mainColor }

    local delta = decodedDelta()
    if delta == nil then
        deltaLabel:set { text = "--" }
    else
        deltaLabel:set { text = string.format("%+d", delta) }
    end

    if age == nil then
        ageLabel:set { text = "--m ago", text_color = COLOR_STALE }
    else
        ageLabel:set {
            text = tostring(age) .. "m ago",
            text_color = stale and COLOR_STALE or COLOR_AGE,
        }
    end

    local iob = decodedIob()
    if iob == nil or stale then
        iobLabel:set { text = "IOB —" }
    else
        iobLabel:set { text = string.format("IOB %.3f U", iob) }
    end

    setStrike(stale, glucoseText)
end

local function subscribe(key, target)
    return pcall(function()
        dataman.subscribe(key, root, function(_, value)
            state[target] = decodeFixed(value)
            state.lastTransportUpdate = os.time()
            render()
        end)
    end)
end

subscribe("weatherCurrentTemperature", "glucose")
subscribe("weatherCurrentHumidity", "age")
subscribe("weatherCurrentAirQualityIndex", "aqi")
subscribe("weatherCurrentUVIndex", "trend")
subscribe("weatherCurrentPressure", "pressure")

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
