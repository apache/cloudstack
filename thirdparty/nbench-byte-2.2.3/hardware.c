#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define BUF_SIZ 1024

/******************
** output_string **
*******************
** Displays a string on the screen.  Also, if the flag
** write_to_file is set, outputs the string to the output file.
** Note, this routine presumes that you've included a carriage
** return at the end of the buffer.
*/
static void output_string(const char *buffer, const int write_to_file,
                          FILE *global_ofile){
  printf("%s",buffer);
  if(write_to_file!=0)
    fprintf(global_ofile,"%s",buffer);
  return;
}


/******************
** removeNewLine **
*******************
** Removes a trailing newline character if present
*/
static void removeNewLine(char * s) {
  if(strlen(s)>0 && s[strlen(s)-1] == '\n') {
    s[strlen(s)-1] = '\0';
  }
}


/***************
** runCommand **
****************
** Run the system command through a pipe
** The pointer result must point to a pre-allocated array of at least BUF_SIZ
*/
static void runCommand (const char *command, char *result) {
  FILE * pipe;

  pipe = popen(command, "r");
  if(pipe == NULL) {
    /* command failed */
    result[0] = '\0';
  } else {
    if(NULL == fgets(result, BUF_SIZ, pipe)){
      /* command failed */
      result[0] = '\0';
    }
    pclose(pipe);
  }
  removeNewLine(result);
}


/********************
** readProcCpuInfo **
*********************
** Reads and parses /proc/cpuinfo on a Linux system
** The pointers must point to pre-allocated arrays of at least BUF_SIZ
*/
static void readProcCpuInfo (char *model, char *cache) {
  FILE * info;
  char * cp;
  int cpus = 0;
  char * buffer_end;
  char buffer[BUF_SIZ];
  char vendor_id[BUF_SIZ];
  char model_name[BUF_SIZ];
  char cpu_MHz[BUF_SIZ];
  int i;
  float f;

  vendor_id[0] = model_name[0] = cpu_MHz[0] = model[0] = cache[0] = '\0';
  info = fopen("/proc/cpuinfo", "r");
  if(info != NULL) {
    /* command did not fail */
    while(NULL != fgets(buffer, BUF_SIZ, info)){
      buffer_end = buffer + strlen(buffer);
      cp = buffer;
      if(! strncmp(buffer, "processor", 9)) {
        cpus++;
      } else if(! strncmp(buffer, "vendor_id", 9)) {
        cp+=strlen("vendor_id");
        while(cp < buffer_end && ( *cp == ' ' || *cp == ':'|| *cp == '\t'))
          cp++;
        if(cp<buffer_end) {
          strcpy(vendor_id, cp);
        }
        removeNewLine(vendor_id);
      } else if(! strncmp(buffer, "model name", 10)) {
        cp+=strlen("model name");
        while(cp < buffer_end && ( *cp == ' ' || *cp == ':'|| *cp == '\t'))
          cp++;
        if(cp<buffer_end) {
          strcpy(model_name, cp);
        }
        removeNewLine(model_name);
      } else if(! strncmp(buffer, "cpu MHz", 7)) {
        cp+=strlen("cpu MHz");
        while(cp < buffer_end && ( *cp == ' ' || *cp == ':'|| *cp == '\t'))
          cp++;
        if(cp<buffer_end) {
          strcpy(cpu_MHz, cp);
        }
        removeNewLine(cpu_MHz);
      } else if(! strncmp(buffer, "cache size", 10)) {
        cp+=strlen("cache size");
        while(cp < buffer_end && ( *cp == ' ' || *cp == ':'|| *cp == '\t'))
          cp++;
        if(cp<buffer_end) {
          strcpy(cache, cp);
        }
        removeNewLine(cache);
      }
    }
    if(cpus>1) {
      if (cpus==2) {
        strcpy(model, "Dual");
      } else {
        sprintf(model, "%d CPU", cpus);
      }
    }
    cp = model + strlen(model);
    if(vendor_id[0] != '\0'){
      if(cp != model){
        *cp++ = ' ';
      }
      strcpy(cp, vendor_id);
      cp += strlen(vendor_id);
    }
    if(model_name[0] != '\0'){
      if(cp != model){
        *cp++ = ' ';
      }
      strcpy(cp, model_name);
      cp += strlen(model_name);
    }
    if(cpu_MHz[0] != '\0'){
      if(cp != model){
        *cp++ = ' ';
      }
      f = atof(cpu_MHz);
      i = (int)(f+0.5f);
      sprintf(cpu_MHz, "%dMHz", i);
      strcpy(cp, cpu_MHz);
      cp += strlen(cpu_MHz);
    }
    fclose(info);
  }
}


/*************
** hardware **
**************
** Runs the system command "uname -s -r"
** Reads /proc/cpuinfo if on a linux system
** Writes output
*/
void hardware(const int write_to_file, FILE *global_ofile) {
  char buffer[BUF_SIZ];
  char os[BUF_SIZ];
  char model[BUF_SIZ];
  char cache[BUF_SIZ];
  char os_command[] = "uname -s -r";
#ifdef NO_UNAME
  os[0] = '\0';
#else
  runCommand(os_command, os);
#endif
  if(NULL != strstr(os, "Linux")) {
    readProcCpuInfo (model, cache);
  } else {
    model[0] = '\0';
    cache[0] = '\0';
  }
  sprintf(buffer, "CPU                 : %s\n", model);
  output_string(buffer, write_to_file, global_ofile);
  sprintf(buffer, "L2 Cache            : %s\n", cache);
  output_string(buffer, write_to_file, global_ofile);
  sprintf(buffer, "OS                  : %s\n", os);
  output_string(buffer, write_to_file, global_ofile);
}


/************************
** main for hardware.c **
*************************
** For testing of code only
** Should be commented out
*/
/*
int main(int argc, char * argv[]) {
  hardware(0, NULL);
  return 0;
}
*/
