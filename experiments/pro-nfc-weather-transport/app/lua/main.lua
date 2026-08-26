local lvgl = require("lvgl")
local dataman = require("dataman")

local W = 336
local H = 480
local ROWS = 8
local PAGE_MS = 5000
local INT_MAX = 2147483647

local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_OK = 0x30D158
local COLOR_DIM = 0x8E8E93
local COLOR_ACCENT = 0x64D2FF

-- Weather fields exposed by Xiaomi watchface dataman on related Vela devices.
-- Unsupported keys are isolated with pcall so one missing field cannot kill the face.
local CANDIDATES = {
    "weatherTemperatureUnit",
    "weatherCurrentTemperature",
    "weatherCurrentTemperatureFahrenheit",
    "weatherCurrentTemperatureFeel",
    "weatherCurrentHumidity",
    "weatherCurrentWeather",
    "weatherCurrentWindDirection",
    "weatherCurrentWindAngle",
    "weatherCurrentWindSpeed",
    "weatherCurrentWindLevel",
    "weatherCurrentAirQualityIndex",
    "weatherCurrentAirQualityLevel",
    "weatherCurrentChanceOfRain",
    "weatherCurrentPressure",
    "weatherCurrentVisibility",
    "weatherCurrentUVIndex",
    "weatherCurrentDressIndex",
    "weatherCurrentSunRiseHour",
    "weatherCurrentSunRiseMinute",
    "weatherCurrentSunSetHour",
    "weatherCurrentSunSetMinute",
    "weatherTodayTemperatureMax",
    "weatherTodayTemperatureMin"
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

local title = lvgl.Label(root, {
    x = 0, y = 16, width = W,
    text = "WEATHER TRANSPORT",
    text_color = COLOR_ACCENT,
    text_font = lvgl.Font("MiSans-Regular", 18),
    text_align = lvgl.ALIGN.TOP_MID,
})

local summary = lvgl.Label(root, {
    x = 0, y = 45, width = W,
    text = "scanning weather sources...",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 14),
    text_align = lvgl.ALIGN.TOP_MID,
})

local pageLabel = lvgl.Label(root, {
    x = 0, y = 68, width = W,
    text = "",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 12),
    text_align = lvgl.ALIGN.TOP_MID,
})

local rowLabels = {}
for i = 1, ROWS do
    rowLabels[i] = lvgl.Label(root, {
        x = 9, y = 94 + (i - 1) * 40, width = W - 18,
        text = "",
        text_color = COLOR_PRIMARY,
        text_font = lvgl.Font("MiSans-Regular", 13),
        text_align = lvgl.ALIGN.TOP_LEFT,
    })
end

local footer = lvgl.Label(root, {
    x = 0, y = 426, width = W,
    text = "Send TEST A / B from BandDrip phone app",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 11),
    text_align = lvgl.ALIGN.TOP_MID,
})

local accepted = {}
local rejected = 0
local page = 1

local function decode(value)
    if value == nil then return "..." end
    if value == INT_MAX then return "INVALID" end
    return tostring(value // 256)
end

local function totalPages()
    local n = math.ceil(#accepted / ROWS)
    if n < 1 then n = 1 end
    return n
end

local function renderPage()
    local pages = totalPages()
    if page > pages then page = 1 end
    pageLabel:set { text = string.format("PAGE %d/%d", page, pages) }

    for row = 1, ROWS do
        local idx = (page - 1) * ROWS + row
        local item = accepted[idx]
        if item then
            local state = item.seen and decode(item.value) or "SUBSCRIBED"
            rowLabels[row]:set {
                text = item.key .. " = " .. state,
                text_color = item.seen and COLOR_OK or COLOR_PRIMARY,
            }
        else
            rowLabels[row]:set { text = "" }
        end
    end
end

local function updateSummary()
    summary:set {
        text = string.format("VALID %d/%d  REJECTED %d", #accepted, #CANDIDATES, rejected)
    }
end

for _, keyName in ipairs(CANDIDATES) do
    local item = { key = keyName, value = nil, seen = false }
    local ok = pcall(function()
        dataman.subscribe(keyName, root, function(_, value)
            item.value = value
            item.seen = true
            renderPage()
        end)
    end)

    if ok then
        table.insert(accepted, item)
    else
        rejected = rejected + 1
    end
end

updateSummary()
renderPage()

local pageTimer = lvgl.Timer {
    period = PAGE_MS,
    cb = function(_)
        local pages = totalPages()
        if pages > 1 then
            page = page + 1
            if page > pages then page = 1 end
            renderPage()
        end
    end,
}

pageOnPause = function()
    pageTimer:pause()
end

pageOnResume = function()
    updateSummary()
    renderPage()
    pageTimer:resume()
end
