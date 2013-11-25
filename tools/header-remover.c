#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main()
{
    // Make the buffer
    int bufSize = 4096;
    char buf[bufSize];
    memset(buf, 0, bufSize * sizeof(char));
    // Gobble the header
    while (fgets(buf, sizeof(buf), stdin))
        if(buf[0] == '\r' && buf[1] == '\n')
            break;
    memset(buf, 0, bufSize * sizeof(char));
    // Safety first
    if (ferror(stdin)) {
        perror(NULL);
        exit(1);
    }
    // Write the file
    size_t bytesRead;
    while ((bytesRead = fread(buf, sizeof(char), bufSize, stdin)) == bufSize)
        fwrite(buf, sizeof(char), bytesRead, stdout);
    if (feof(stdin)) {
        if (bytesRead > 4 && bytesRead != bufSize)
            fwrite(buf, sizeof(char), bytesRead - 4, stdout);
    } else if (ferror(stdin)) {
        perror(NULL);
        exit(1);
    }
    return 0;
}
