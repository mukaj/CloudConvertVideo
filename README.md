## CloudConvertVideo
This project was made to simplify the complex nature of preparing a video/audio file for HTML5 so that it can be viewed on any device.
### How it works
It takes a file from an URL or from a local directory, and detects if it's an audio/video file using Jave (Java ffmpeg wapper).
After it detects the kind of file, it sends that file to CloudConvert for conversions and it creates all versions of that file that are needed for the file to satisfy all the different HTML5 filetype requirements.


*NOTE: Remember to put your API key in the CloudConvert.java class file before running the code otherwise **it will not work.***
