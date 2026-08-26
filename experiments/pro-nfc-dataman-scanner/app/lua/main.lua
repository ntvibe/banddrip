local lvgl = require("lvgl")
local dataman = require("dataman")

local W = 336
local H = 480
local ROWS = 9
local PAGE_MS = 5000
local INT_MAX = 2147483647

local COLOR_BG = 0x000000
local COLOR_PRIMARY = 0xFFFFFF
local COLOR_OK = 0x30D158
local COLOR_DIM = 0x8E8E93
local COLOR_ACCENT = 0x64D2FF

-- Mix of keys documented on newer Vela builds plus older Xiaomi watchface
-- keys that are interesting for a phone-writable BandDrip transport.
-- pcall() is intentional: unsupported keys must not crash the face.
local CANDIDATES = {
    "timeHour", "timeMinute", "timeSecond",
    "dateDay", "dateMonth", "dateWeek",
    "systemStatusBattery", "systemStatusCharge",
    "systemStatusDisturb", "systemStatusBluetooth",
    "systemStatusWifi", "systemStatusScreenLock",
    "healthStepCount", "healthHeartRate", "healthCalorie",
    "healthStandCount", "healthOxygenSpO2", "healthPressureIndex",
    "healthSleepDuration", "healthExerciseDuration", "healthEnergyConsumed",
    "systemSensorFusionAltitude",
    "miscIs24H",
    "AppAlarmHour", "AppAlarmMinute",
    "batteryLevel", "step", "heartRate", "calorie"
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
    x = 0, y = 18, width = W,
    text = "DATAMAN SCANNER",
    text_color = COLOR_ACCENT,
    text_font = lvgl.Font("MiSans-Regular", 19),
    text_align = lvgl.ALIGN.TOP_MID,
})

local summary = lvgl.Label(root, {
    x = 0, y = 48, width = W,
    text = "scanning...",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 15),
    text_align = lvgl.ALIGN.TOP_MID,
})

local pageLabel = lvgl.Label(root, {
    x = 0, y = 72, width = W,
    text = "",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 13),
    text_align = lvgl.ALIGN.TOP_MID,
})

local rowLabels = {}
for i = 1, ROWS do
    rowLabels[i] = lvgl.Label(root, {
        x = 13, y = 96 + (i - 1) * 37, width = W - 26,
        text = "",
        text_color = COLOR_PRIMARY,
        text_font = lvgl.Font("MiSans-Regular", 15),
        text_align = lvgl.ALIGN.TOP_LEFT,
    })
end

local footer = lvgl.Label(root, {
    x = 0, y = 440, width = W,
    text = "invalid keys are safely skipped",
    text_color = COLOR_DIM,
    text_font = lvgl.Font("MiSans-Regular", 12),
    text_align = lvgl.ALIGN.TOP_MID,
})

local accepted = {}
local rejected = 0
local page = 1

local function decode(value)
    if value == nil then return "..." end
    if value == INT_MAX then return "INVALID" end
    local n = value // 256
    return tostring(n)
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
                text = item.key .. "  =  " .. state,
                text_color = item.seen and COLOR_OK or COLOR_PRIMARY,
            }
        else
            rowLabels[row]:set { text = "" }
        end
    end
end

local function updateSummary()
    summary:set {
        text = string.format("VALID %d / %d   REJECTED %d", #accepted, #CANDIDATES, rejected)
    }
end

-- Important: each subscribe is isolated. The previous probe proved that this
-- firmware throws an error for AppAlarmHour; pcall keeps scanning after it.
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
