# Pirocast
*Transform your Raspberry Pi into an autonomous radio receiver!*  
  
This piece of software allows you to use your Raspberry Pi as a radio receiver and/or simple audio player!  
Pirocast is inspired by typical car radio systems. It can be operated with just 3 buttons and an I2C LCD display.

# Features
- **Easy to setup and use.**  
All you need is Java and GnuRadio installed!
- **Configurable**  
You can configure almos **everything**! From time and date formats, labels, default and allowed settings, to bands, sources and logging
- **Support for various audio sources**:
  - **Live radio** (SDR required)  
  Listen to your local FM radio stations.  
  Supports FM, AM and Narrowband FM modulations.  
  Also supports **RDS** (powered by gr-rds) with station name, radiotext, Traffic Announcement and Traffic Programme.  
  Supports **APRS** decoding for narrowband FM via Direwolf.
  - **Internet radio**  
  Stream music directly from your favourite internet radio stations. Supports most major audio formats (via ffmpeg)
  - **Local files**  
  Pirocast can scan audio files in configured directories and play them. Supports most major audio formats.
  - **AUX**  
  If you have an audio input in your Raspberry Pi, you can loop it back to output with Pirocast's AUX source.
- **Extensible**  
The source code can be easily tweaked to add new features, such as new audio sources, modulations, etc.

# Requirements
### Software
- Java >= 17
- GnuRadio (Optional) with additional packages:
  - `gr-rds` - for RDS decoding
  - `gr-osmosdr` - for SDR support  
- Direwolf (Optional) - for APRS decoding
- FFmpeg (Optional) - for internet radio and local audio playe

### Hardware
- A HD44780 display with I2C converter
- 3 buttons with 3 1K resistors, one resistor per button
- an SDR receiver (tested with [RTL-SDR](https://www.rtl-sdr.com/))  
*Optional if you don't need live radio*

Note that any hardware is *optional* if you want to run Pirocast for development.  
Pirocast has a built-in LCD display emulator.