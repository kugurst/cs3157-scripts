#!/bin/sh

#constants
srcPkg="gedit-source-code-browser-plugin"

#Preliminary Actions
echo Making sure gconftool and gsettings are installed as we will need them later.
#There shouldn't be errors...
suppress=$(sudo apt-get install gconf2 libglib2.0-bin)
mkdir -p ~/.temp
cd ~/.temp

#Let's install gedit-plugins
echo "Installing gedit-plugins (if not already installed)..."
checkError=$(sudo apt-get install gedit-plugins 2>&1 | grep E:)
if [ ${#checkError} != 0 ]
then
	echo Unable to install gedit-plugins. Please do so manually.
else
	echo Gedit Plugins installed successfully.
fi

#Let's try to install gedit-source-code-browser-plugin
notFound=$(sudo apt-get install $srcPkg 2>&1 | grep "Unable to locate package $srcPkg")
#If it wasn't able to locate the package, let's download it
found=true
if [ ${#notFound} != 0 ]
then
	found=false
	echo "Unable to install $srcPkg. Will install manually."
else
	echo Source Code Browser installed successfully.
fi

#Let's download the Pair Character Complete Plugin
echo Downloading the Pair Character Complete Plugin
wget https://gedit-pair-char-autocomplete.googlecode.com/files/gedit-pair-char-completion-1.0.5-gnome3.tar.gz
tar xf gedit-pair-char-completion-1.0.5-gnome3.tar.gz
cd gedit-pair-char-completion-1.0.5-gnome3/
sh ./install.sh

#If found is false, let's install the Source Code Browser
if [ $found = false ]
then
	echo Downloading the Source Code Browser Plugin
	git clone https://github.com/Quixotix/gedit-source-code-browser.git scb
	cd scb
	cp sourcecodebrowser.plugin ~/.local/share/gedit/plugins/
	cp -R sourcecodebrowser ~/.local/share/gedit/plugins/
	
	#Now for the dconf schema
	cd ~/.local/share/gedit/plugins/sourcecodebrowser/data/
	sudo cp org.gnome.gedit.plugins.sourcecodebrowser.gschema.xml /usr/share/glib-2.0/schemas/
	sudo glib-compile-schemas /usr/share/glib-2.0/schemas/
	echo Installed Source Code Browser.
fi

echo "Configuring gedit..."
#Get the current terminal background_color and foreground_color
bgColor=$(gconftool-2 --get /apps/gnome-terminal/profiles/Default/background_color)
fgColor=$(gconftool-2 --get /apps/gnome-terminal/profiles/Default/foreground_color)

#Activating my plugins
gsettings set org.gnome.gedit.plugins active-plugins "['multiedit', 'filebrowser', 'sourcecodebrowser', 'codecomment', 'docinfo', 'terminal', 'modelines', 'colorpicker', 'sessionsaver', 'quickopen', 'time', 'spell', 'wordcompletion', 'pair_char_completion']"

#Setting the appropriate keys for gedit
gsettings set org.gnome.gedit.plugins.terminal palette ''
gsettings set org.gnome.gedit.plugins.terminal background-color $bgColor
gsettings set org.gnome.gedit.plugins.terminal foreground-color $fgColor
gsettings set org.gnome.gedit.preferences.editor auto-indent 'true'
gsettings set org.gnome.gedit.preferences.editor bracket-matching 'true'
gsettings set org.gnome.gedit.preferences.editor display-line-numbers 'true'
gsettings set org.gnome.gedit.preferences.editor highlight-current-line 'true'
gsettings set org.gnome.gedit.preferences.editor scheme 'cobalt'
gsettings set org.gnome.gedit.preferences.editor tabs-size '4'
gsettings set org.gnome.gedit.preferences.ui bottom-panel-visible 'true'
gsettings set org.gnome.gedit.preferences.ui side-panel-visible 'true'
gsettings set org.gnome.gedit.preferences.ui statusbar-visible 'true'

#Clean up
rm -rf ~/.temp

echo "Done!"
