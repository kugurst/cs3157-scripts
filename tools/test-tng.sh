#!/bin/bash
# For each uni in this folder
dir=../unis/*
for file in $dir; do
    if [ -d "$file" ]; then
        uni="${file##*/}"
        tng="$file/TNG.txt"
        rm -f "$tng"
        index=$(echo -ne "GET /~$uni/cs3157/tng/ HTTP/1.0\r\nHost: www.cs.columbia.edu\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.5\r\nAccept-Encoding: gzip, deflate\r\n\r\n" | nc "www.cs.columbia.edu" 80)
        if [[ "$index" == *"images/crew.jpg"* ]] && [[ "$index" == *"images/ship.jpg"* ]]; then
            echo "Proper image paths specified." >> "$tng"
        fi

        # Now the images
        image="$file/image.jpg"
        echo -ne "GET /~$uni/cs3157/tng/images/crew.jpg HTTP/1.0\r\nHost: www.cs.columbia.edu\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0\r\nAccept: image/png,image/*;q=0.8,*/*;q=0.5\r\nAccept-Language: en-US,en;q=0.5\r\nAccept-Encoding: gzip, deflate\r\nReferer: http://www.cs.columbia.edu/~ma2799/cs3157/tng/\r\n\r\n" | nc "www.cs.columbia.edu" 80 > "$image"
        line=$(head -n 1 "$image")
        if [[ "$line" == *"200 OK"* ]]; then
            echo "crew.jpg found." >> "$tng"
        fi

        echo -ne "GET /~$uni/cs3157/tng/images/ship.jpg HTTP/1.0\r\nHost: www.cs.columbia.edu\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0\r\nAccept: image/png,image/*;q=0.8,*/*;q=0.5\r\nAccept-Language: en-US,en;q=0.5\r\nAccept-Encoding: gzip, deflate\r\nReferer: http://www.cs.columbia.edu/~ma2799/cs3157/tng/\r\n\r\n" | nc "www.cs.columbia.edu" 80 > "$image"
        line=$(head -n 1 "$image")
        if [[ "$line" == *"200 OK"* ]]; then
            echo "ship.jpg found." >> "$tng"
        fi
    fi
done

