#!/bin/bash
# Setting up some variables
port=$(cat http-port.txt)
# We don't want to resolve our own symlink
progDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
htmlDir=./htdocs
ncDir="test"
indexFile="index.html"

# Cleaning the test directory
rm -rf "$ncDir"
mkdir "$ncDir"

fileArr=( "" "css/style.css" "shared/previews.css"
"js/jquery/jquery-1.5.2.min.js" "js/xenforo/xenforo.js?_v=bba17b4a"
"images/great-hairstyle.jpg" "images/hairstyle17.jpg"
"images/hairstyle18.jpg" "images/hairstyle19.jpg" "images/hairstyle20.jpg"
"images/featured.jpg" "images/skinhead.png" "images/website/large/0_12.png"
"js/jquery/jquery-1.5.2.min.js" "js/xenforo/xenforo.js?_v=bba17b4a"
"images/website/large/0_12.png" "images/bg-header.gif" "images/bg-logo.png"
"images/bg-nav-right-selected.gif" "images/bg-nav-left-selected.gif"
"images/bg-nav-right.gif" "images/bg-nav-left.gif" "images/frame.png"
"fonts/Arimo-Bold/Arimo-Bold.woff" "fonts/ChangaOne/ChangaOne-Regular.woff"
"fonts/Comfortaa/Comfortaa-Regular.woff" "images/bg-featured.png"
"images/icons.png" "fonts/Arimo-Bold/Arimo-Bold.ttf"
"fonts/ChangaOne/ChangaOne-Regular.ttf" "fonts/Comfortaa/Comfortaa-Regular.ttf"
"favicon.ico" )

# Removing the DIFF file, as we append to it
rm -f "DIFF.txt"

# For each file
for file in "${fileArr[@]}"; do
    if [[ "$file" == *"/"* ]]; then
        filePath=${file%/*}
        mkdir -p "$ncDir/$filePath"
    fi

    # Send an HTTP request in firefox style (to be specific, my Firefox's headers)
    echo -ne \
        "GET /$file HTTP/1.1\r\n" \
        "Host: localhost:$port\r\n" \
        "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0\r\n" \
        "Accept: */*\r\n" \
        "Accept-Language: en-US,en;q=0.5\r\n" \
        "Accept-Encoding: gzip, deflate\r\n" \
        "Referer: http://localhost:$port/\r\n" \
        "Connection: keep-alive\r\n" \
        "\r\n" \
    | nc localhost 8888 | "$progDir/header-remover" > "$ncDir/${file:-$indexFile}"
    
    # Diff each file and record the differences, for browser-less verification
    echo "${file:-$indexFile}:" >> "DIFF.txt"
    diff "$htmlDir/${file:-$indexFile}" "$ncDir/${file:-$indexFile}" >> "DIFF.txt" 2>&1
    echo "-----------------------------------------------------------" >> "DIFF.txt"
done

# Now for mdb-lookup-server

fileArr=( "mdb-lookup" "mdb-lookup?key=" "mdb-lookup?key=hi"
"mdb-lookup?key=Hodor" "mdb-lookup?key=blargh" )

for file in "${fileArr[@]}"; do
    echo -ne \
        "GET /$file HTTP/1.1\r\n" \
        "Host: localhost:$port\r\n" \
        "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0\r\n" \
        "Accept: */*\r\n" \
        "Accept-Language: en-US,en;q=0.5\r\n" \
        "Accept-Encoding: gzip, deflate\r\n" \
        "Referer: http://localhost:$port/\r\n" \
        "Connection: keep-alive\r\n" \
        "\r\n" \
    | nc localhost 8888 | "$progDir/header-remover" > "$ncDir/$file"
done

# Throw in an empty request to "mimic" firefox's keep-alive
nc -z localhost 8888

# Now, make another request to see how the server handled this
file="mdb-lookup?key=Jae"
echo -ne \
    "GET /$file HTTP/1.1\r\n" \
    "Host: localhost:$port\r\n" \
    "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0\r\n" \
    "Accept: */*\r\n" \
    "Accept-Language: en-US,en;q=0.5\r\n" \
    "Accept-Encoding: gzip, deflate\r\n" \
    "Referer: http://localhost:$port/\r\n" \
    "Connection: keep-alive\r\n" \
    "\r\n" \
| nc localhost 8888 | "$progDir/header-remover" > "$ncDir/$file"

