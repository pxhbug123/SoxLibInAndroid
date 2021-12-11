# SoxLibInAndroid

I edited some sox source file,such as sox.c sox.h formats.c and CMakeLists.txt. you can Use Library like command.

### Graddle Installation
1. Download the ``soxcommandlibrary-release.aar`` file onto your computer.
2. Then in your app module build.gradle file, add the path to the downloaded file like the example below.

```gradle
dependencies {
  implementation files('/Users/Bob/path/to/file/soxcommandlibrary-release.aar')
}
```
3. Sync the project


**Example usage:**
```Java

    import com.vtech.audio.helper.SoxCommandLib;

    // Simple combiner
    // sox file[0] file[1] ... file[n] <outFile>

    String soxCommand = "sox -m in1.mp3 in2.mp3 in3.mp3 out.mp3"
    int exitVal = SoxCommandLib.executeCommand(soxCommand);
 
    if(exitVal == 0){
      Log.v("SOX", "Sox finished successfully");
    }else{
      Log.v("SOX", "Sox failed");
    }
 
```
