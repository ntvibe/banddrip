local lvgl = require("lvgl")
-- P0: capability observations only. No module loading, firmware writes,
-- network access, sensor subscriptions, or fake glucose values.
local root = lvgl.Object(nil, {
    w = lvgl.HOR_RES(), h = lvgl.VER_RES(), bg_color = 0,
    bg_opa = lvgl.OPA(100), border_width = 0, pad_all = 0,
})
root:clear_flag(lvgl.FLAG.SCROLLABLE)
root:add_flag(lvgl.FLAG.EVENT_BUBBLE)
local title = lvgl.Label(root, { x=10, y=15, width=316,
    text="BANDDRIP TRANSPORT P0", text_color=0x64D2FF,
    text_font=lvgl.Font("MiSans-Regular",18) })
local rows = {}
for n=1,10 do
    rows[n] = lvgl.Label(root, {x=10,y=57+(n-1)*36,width=316,
        text="",text_color=0xFFFFFF,text_font=lvgl.Font("MiSans-Regular",16)})
end
local footer=lvgl.Label(root,{x=10,y=433,width=316,text="Capability test; no glucose data",
    text_color=0x8E8E93,text_font=lvgl.Font("MiSans-Regular",14)})
local lines={}
local function add(s) lines[#lines+1]=s end
local function read(path)
    if type(io)~="table" or type(io.open)~="function" then return nil end
    local ok,data=pcall(function()
        local f=io.open(path,"r")
        if not f then return nil end
        local value=f:read(8192)
        f:close()
        return value
    end)
    return ok and data or nil
end
local function capture(command,tag)
    -- Only constant read-only shell queries below; only our temp file is written.
    local path="/data/banddrip-p0-"..tag..".tmp"
    if type(os)~="table" or type(os.execute)~="function" then return nil end
    if type(os.remove)=="function" then pcall(os.remove,path) end
    local ok=pcall(os.execute,command.." > "..path)
    local result=ok and read(path) or nil
    if type(os.remove)=="function" then pcall(os.remove,path) end
    return result
end
local function has(text,word)
    return text and text:match("%f[%w_]"..word.."%f[^%w_]") and "YES" or "NOT SEEN"
end
local function probe()
    add("io.open: "..(type(io)=="table" and type(io.open) or "absent"))
    add("os.execute: "..(type(os)=="table" and type(os.execute) or "absent"))
    local fw=capture("getprop ro.build.version","fw")
    add("FW: "..(fw and fw:gsub("%s+"," "):sub(1,24) or "UNREADABLE"))
    local help=capture("help","help")
    add("Shell output: "..(help and #help>0 and "READABLE" or "UNAVAILABLE"))
    add("insmod command: "..has(help,"insmod"))
    add("curl command: "..has(help,"curl"))
    add("quickjs command: "..has(help,"quickjs"))
    local uorb=capture("ls /dev/uorb","uorb")
    add("uORB listing: "..(uorb and uorb:find("/") and "READABLE" or "CHECK PAGE 2"))
    add("No transport proven yet")
    add("Next page: matching names")
    -- Names only, never read sensor/device nodes: reads may block.
    add("uORB glucose-related names:")
    local found=false
    if uorb then
        for line in uorb:gmatch("[^\r\n]+") do
            if line:lower():find("glucose") or line:lower():find("sugar") then
                add(line:sub(1,34)); found=true
            end
        end
    end
    if not found then add("None seen (not proof of absence)") end
    add("No modules loaded")
    add("No firmware addresses used")
    add("Photograph both pages")
end
local page=1
local function render()
    local pages=math.max(1,math.ceil(#lines/10))
    for n=1,10 do rows[n]:set{text=lines[(page-1)*10+n] or ""} end
    title:set{text="BANDDRIP P0  "..page.."/"..pages}
    page=page%pages+1
end
local ran=false
local timer=lvgl.Timer{period=8000,cb=function()
    if not ran then
        ran=true
        local ok=pcall(probe)
        if not ok then add("Probe error; no transport claim") end
    end
    render()
end}
rows[1]:set{text="Reading capabilities in 8 sec..."}
pageOnPause=function() timer:pause() end
pageOnResume=function() timer:resume() end
