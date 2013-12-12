#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#define DMP(...) if (strDmp) { __VA_ARGS__; }
#define DMPSTAT(...) if(statStrDmp) { __VA_ARGS__; }

int verifyHeader(char *, int, int *);
int isNumber(char *);
void gobbleStdin(char *, int);

char *delim = " ";
FILE *statStrDmp = NULL;

int main(int argc, char **argv)
{
    // Make the file dump
    FILE *strDmp = NULL;
    // fprintf(stderr, "argc: %d\n", argc);
    if (argc > 1) {
        // fprintf(stderr, "argv: %s\n", argv[1]);
        strDmp = fopen(argv[1], "wb");
        if (strDmp == NULL) {
            perror(NULL);
            exit(1);
        }
        statStrDmp = strDmp;
    }
    // Make the buffer
    int bufSize = 4096;
    char buf[bufSize];
    int bufBlock = sizeof(char) * bufSize;
    memset(buf, 0, bufBlock);
    // Indicates if the header line was bad or if we are reading in the first header
    int line = 1;
    int blankFound = 0;
    // Gobble (and verify) the header
    while (fgets(buf, sizeof(buf), stdin)) {
        int len = strlen(buf);
        DMP(fwrite(buf, sizeof(char), len, strDmp));
        if (len < 2) {
            line = 1;
            break;
        }
        if (buf[0] == '\r' && buf[1] == '\n' && buf[2] == '\0') {
            blankFound = 1;
            break;
        }
        if (!verifyHeader(buf, bufSize, &line))
            break;
    }
    memset(buf, 0, bufBlock);
    // Safety first
    if (ferror(stdin)) {
        DMP(fclose(strDmp))
        perror(NULL);
        exit(1);
    }
    // Gobble the rest of stdin if the headers were bad
    if (line || !blankFound) {
        gobbleStdin(buf, bufSize);
        DMP(fclose(strDmp))
        return 1;
    }
    // Write the file
    size_t bytesRead;
    while ((bytesRead = fread(buf, sizeof(char), bufSize, stdin)) == bufSize)
        fwrite(buf, sizeof(char), bytesRead, stdout);
        DMP(fwrite(buf, sizeof(char), bytesRead, strDmp));
    if (feof(stdin) && bytesRead > 0) {
        fwrite(buf, sizeof(char), bytesRead, stdout);
        DMP(fwrite(buf, sizeof(char), bytesRead, strDmp));
    } else if (ferror(stdin)) {
        DMP(fclose(strDmp))
        perror(NULL);
        exit(1);
    }
    DMP(fclose(strDmp))
    return 0;
}

int verifyHeader(char *buf, int bufSize, int *stat)
{
    int bufLen = strlen(buf);
    char* tok = strtok(buf, delim);
    // We're parsing the first header
    if (*stat == 1) {
        // HTTP/1.0
        if (strcmp("HTTP/1.0", tok) != 0)
            return 0;
        // 200
        tok = strtok(NULL, delim);
        if (!isNumber(tok))
            return 0;
        // Go to the rest of the line (manually)
        int len = strlen(tok);
        tok += len + 1;
        // Make sure we're not out of bounds
        if (tok > buf + bufLen)
            return 0;
        // OK (or anything, really. I'm not picky)
        len = strlen(tok);
        if (len < 2 || !isalpha(tok[0]) || tok[len - 2] != '\r' || tok[len - 1] != '\n')
            return 0;
        return !(*stat = 0);
    } else {
        // Host:
        int len = strlen(tok);
        if (tok[len - 1] != ':')
            return !(*stat = 1);
        tok += len + 1;
        // Make sure we're not out of bounds
        if (tok > buf + bufLen)
            return !(*stat = 1);
        // Rest of the line
        len = strlen(tok);
        if (len < 2 || tok[len - 2] != '\r' || tok[len - 1] != '\n')
            return !(*stat = 1);
        // Good ending
        return 1;
    }
}

void gobbleStdin(char *buf, int bufSize)
{
    // Check for feof
    while (!feof(stdin)) {
        if (ferror(stdin))
            return;
        int bytesRead = 0;
        if ((bytesRead = fread(buf, sizeof(char), bufSize, stdin)) > 0)
            DMPSTAT(fwrite(buf, sizeof(char), bytesRead, statStrDmp));
    }
}

int isNumber(char *num) {
    if (!num)
        return 0;
    int pos = 0;
    while (num[pos] != '\0' && num[pos] >= '0' && num[pos] <= '9')
        pos++;
    if (num[pos] == '\0')
        return 1;
    return 0;
}

