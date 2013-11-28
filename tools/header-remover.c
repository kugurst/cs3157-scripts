#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main()
{
    // Make the buffer
    int bufSize = 4096;
    char buf[bufSize];
    int bufBlock = sizeof(char) * bufSize;
    memset(buf, 0, bufBlock);
    // Gobble the header
    while (fgets(buf, sizeof(buf), stdin))
        if(buf[0] == '\r' && buf[1] == '\n' && buf[2] == '\0')
            break;
    memset(buf, 0, bufBlock);
    // Safety first
    if (ferror(stdin)) {
        perror(NULL);
        exit(1);
    }
    // Write the file
    size_t bytesRead;
    while ((bytesRead = fread(buf, sizeof(char), bufSize, stdin)) == bufSize)
        fwrite(buf, sizeof(char), bytesRead, stdout);
    if (feof(stdin) && bytesRead > 0) {
        fwrite(buf, sizeof(char), bytesRead, stdout);
    } else if (ferror(stdin)) {
        perror(NULL);
        exit(1);
    }
    return 0;
}
