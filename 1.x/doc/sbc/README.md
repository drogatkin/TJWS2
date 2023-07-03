# No Raspberry Pi until 2024, so an alternative

## Why look for an alternative
I was waiting for Raspberry Pi 4 since 2021. No Raspberry Pi for 2022 the Raspberry Pi site told, however when I got the same message for 2023, I decide to look for alternatives.
 Unfortunately, all alternatives looked more expensive and sometimes significantly. Orange Pi 5 looked reasonably priced from a vendor from China. 
It costs $85 for 8Gb version. Raspberry Pi costs $75 for the same amount of memory. You can buy Orange Pi 5 8 Gb from Amazon or AliExpress for $95 delivered plus tax currently.
 So I decide to give it a try. The delivery took about 3 weeks from AliExpress. It should be much faster from Amazon. AliExpress will start heavily spam you after you did a purchase, 
so share some spare e-mail address with them when do a purchase. 

## Where to get software
Although Orange Pi website designed well and uses a responsive approach, it’s making difficult to see details on a phone. It is also not obvious how to find a download
 page for the SBC software. I provide a direct [link](http://www.orangepi.org/html/hardWare/computerAndMicrocontrollers/service-and-support/Orange-pi-5.html).
You can obtain different flavors of Linux or Android there. Using Google drive for most downloads isn’t a good idea, because it will require you to login with some Google account
 and doesn’t work for mobile devices. However you need to deal with that. I downloaded Ubuntu server edition since plan to use the computer as a server. 

## Preparing micro SD card
Orange Pi 5 can use micro SD card or M.2 drive as a boot source. I tried a micro SD  card first. Writing OS image on SD card is quite easy on Linux systems.
  Remove any SD card from a computer and then execute:

> df -h

Insert  the SD card and again execute:
> df -h
---

`tmpfs           3.2G  152K  3.2G   1% /run/user/1000`

`/dev/mmcblk0p1   30G  9.6G   20G  33% /media/dmitriy/new year`

---
You will notice a new device in the list. Remember the device. Unmount the device executing:

> sudo umount /dev/mmcblk0p1

Note that the device  you just discovered.

You can copy an image now using:

> sudo dd bs=4M of=/dev/mmcblk0 if=/home/dmitriy/Downloads/Orangepi5_1.1.6_ubuntu_focal_server_linux5.10.110.img

**Important**, do not specify partition number as **p1** in the device name. Execute `sync` after you finish copying. If your Linux has GUI,
 then you can use the standard image writer. Do the right mouse click on an image file in the file explorer. Select Open with other
 application and then select the image writer. Follow on screen instructions then.

## Resizing the card

A good news that most Orange-pi OSes will resize OS micro SD card storage to SD card capacity automatically at the first run. My first use case of Orange-pi is a server machine. So when I powered the machine at first time, it booted and then resized the SD card.
You can change some system settings using the utility *orangepi-config*. The utility can take care of several tasks mentioned below.

## ssh
Although a server box is accessed remotely, it’s recommended to have a monitor and a keyboard connected to the Orange Pi box. You need a knowledge of a host name and password for a remote connection. Default values are:

> host: orangepi5

> password: orangepi

It is aligned with default values for Raspberry Pi. Obviously, you can modify the values at your preference after the first run of Ubuntu. Use command:

> passwd

to change password.

You will need to modify files `/etc/hostname` and then `/etc/hosts` to specify a new hostname.  Reboot the system to get changes applied.

Change `hostname` in file `/etc/rc.conf` for **FreeBSD**.

## Timezone

The OS image came with timezone preset in China region. You can modify it to your region using command:

> sudo timedatectl set-timezone \<your timezone\>

For example:

> sudo timedatectl set-timezone America/Los_Angeles

Name of your timezone you can validate using:

> timedatectl list-timezones

You can also use the config utility for that mentioned above.

Setting timezone in  **FreeBSD** uses the command:

> tzsetup America/Los_Angeles


## Java

Most of my server software using Java. So the first step is loading JVM to the machine. I would recommend to install Oracle JVM 17 . You can download the JVM archive in the home directory using command;

> wget https://download.oracle.com/java/17/latest/jdk-17_linux-aarch64_bin.tar.gz

You can install also the current OpenJDK JVM using `sudo apt install default-jdk`. However if you do the Oracle JVM install, I install downloaded .gz using: 

> sudo tar -xzf jdk-17_linux-aarch64_bin.tar.gz -C /var/cache

Or open the archive and then move it in the target directory:

> sudo mv jdk-17.0.7 /var/cache

Very unlikely you run Java on this machine directly, so I don't provide instructions how to simplify that.

**Note**: if you play sound  from Java, then currently Oracle JVM has a problem with that. You will need to install OpenJDK then as described above. 
To assure that a particular audio card is used for your Java playback, create file `.asoundrc` in `$HOME` directory (/root for a service) and put one line there:

> pcm.!default "plughw:1,0"

Use a desired output card number, for example card number 1 for HDMI.

Using Java 11 from Orange Pi Ubuntu distribution will require to add `-Djava.awt.headless=true` in the Java command.

You can install only OpenJDK Java when you use FreeBSD. Switch to `root` using `su`. Issue:

> pkg search ^openjdk

And then install a desired version from the list using:

> pkg install openjdk8

And then follow on screen instructions.


## TJWS

Most of my server applications are Java web applications. Therefore I need to install a Java web container. I use TJWS, however you can use any other web server with a servlet container support. Create a directory for TJWS

> sudo mkdir /usr/local/tjws

Change ownership to a regular user to avoid using *sudo* for any manipulations with the directory:

> sudo chown orangepi /usr/local/tjws

Create a directory for TJWS libraries

> mkdir /usr/local/tjws/lib

Create a directory for logs

> sudo mkdir /var/log/tjws

And allow to use it for a non root server run:

> sudo chown orangepi /var/log/tjws

Create a directory for web applications

> sudo mkdir -p /usr/local/share/java/webapps

And allow a regular user to modify it :

> sudo chown orangepi /usr/local/share/java/webapps

You can copy TJWS jar/zip files now. It can be done using `scp` command or downloading lib files using `wget` or `curl`. I use `wget` because  I have always a machine running TJWS and it’s the easiest way. 
The following files required to run TJWS as an application server:

| Name   | Comment |
| -------- | ------- |
| antlr-2.7.2.jar | CORBA AS |
| app.jar | TJWS AS |
| class-scanner.jar | TJWS WebSocket |
| conn_chk.jar | TJWS JDBC |
| idl.jar | CORBA AS |
| jacorb-3.9.jar | CORBA AS |
| jacorb-omgapi-3.9.jar | CORBA AS |
| jacorb-services-3.9.jar | CORBA AS |
| jasper.jar  | JSP jasper-8.5.78 |
| javax.servlet-api-3.1.0.jar | Servlet API |
| javax.websocket-api-1.1.jar | WebSocket API |
| picocontainer-1.2.jar | CORBA AS |
| servlet-api-2.3.jar | Old servlet API |
| slf4j-api-1.7.14.jar | CORBA AS |
| slf4j-jdk14-1.7.14.jar | CORBA AS |
| stub.jar | CORBA AS |
| war.jar | TJWS war deployment |
| webserver.jar | TJWS |
| wrapper-3.1.0.jar | CORBA AS |
| wskt.jar | TJWS WebSocket |

**Note** that you do not need CORBA 3rd party libraries when you use Java 8.

Copy starting server script `tjwserv-op` [3] to directory `/usr/local/tjws`. Make sure that the script has an execution permission. For example:

> chmod +x tjwserv-op


## Running TJWS as a service

It is the preferred way of running of TJWS because it starts automatically at a boot time of a machine. There are two ways of running a service in Linux: part of **init.d** or **systemd** process.
 Both methods are described in TJWS documentation. I remind just the systemd method because it is the preferred method nowadays.

Copy `tjwserv.service` [2] from TJWS source bin directory  to `/usr/lib/systemd/system` of the target machine. For example:

> sudo cp tjwserv.service /usr/lib/systemd/system/

 Make sure that the location of TJWS start script is correct in  `tjwserv.service`. You can enable the service after executing:

> sudo systemctl enable tjwserv

You can start, stop or restart the service using:

> sudo systemctl [start|stop] tjwserv

If you are willing to delete the service, then execute:

> sudo systemctl disable tjwserv

**Note for FreeBSD** 

FreeBSD uses the approach similar to `init.d`. Copy the service script to directory `rc.d`. Or directory `/usr/local/etc/rc.d` can be used for ports.

> \# cp tjwserv /etc/rc.d

Start the service using:

> \# service tjwserv start

Add below line in `/etc/rc.conf` to make the service start at the boot time:

    tjwserv_enable="YES"

## Connecting USB drive

Even if you install M.2 drive for Orange-pi , then using an external USB drive can be useful. Unless you use a GUI version of Linux, you need to mount USB  drive manually.
 The following steps can be required to mount the drive automatically at boot time.

Create a directory which presents a mount point, for example:

> sudo mkdir /media/exhdd

Name of the directory can be an arbitrary valid Linux path. Mount the drive using command:

> sudo mount /dev/sda1 /media/exhdd

Use command :

> lsblk -o NAME,FSTYPE,UUID,MOUNTPOINT

to find a mount point name.

You need to edit `/etc/fstab` to make the mount permanent. Add a line like:

> /dev/sda1 /media/exhdd auto auto

Note that if you use any GUI, then more likely automatic mount will be assured by GUI underline functionality. And finally if you connect several devices and do not want to mess up which one is which especially if you do remount, then you can use UUID of the disk instead of /dev/sdaX, e.g. UUID=f46df6fd-d541-441c-b2a5-29f8e4af2aa4. UUID and other disk identifications can be found under /dev/disk.  UUID will be also displayed in a result of command `lsblk`.

## FreeBSD notes
Generally all said above is applied for FreeBSD. Since there is no `bash`, scripts have to use `#!/bin/sh`. FreeBSD has no `sudo` by default, so most settings will require switching to `root` using `su`. You can install also `sudo`.

### NTP in FreeBSD
Edit

> \# nano /etc/rc.conf

Add lines: 

    ntpd_enable="YES"
    ntpd_sync_on_start="YES"

You may need also to edit `/etc/ntp.conf` to specify your regional NTP server.

## References
1. [Guide to run TJWS on SBC](https://tjws.sourceforge.net/arch-raspi-java8.html) ([source of the file you can find at](https://github.com/drogatkin/TJWS2/blob/master/1.x/html/arch-raspi-java8.html))
2. [tjwserv.service](https://github.com/drogatkin/TJWS2/blob/master/1.x/bin/tjwserv.service)
3. [tjwserv-op](https://github.com/drogatkin/TJWS2/blob/master/1.x/bin/tjwserv-op)


