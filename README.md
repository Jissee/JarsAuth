# JarsAuth

<ol>
<li> What is JarsAuth? </li>  

JarsAuth is a Minecraft mod to check whether the mod-pack is modified from the original build.   

<li> How it works?</li>

JarsAuth will check all files in the configured folders and calculate their hash value, which is used to generate the hash code of the whole client.
Then this hash code will be used to authenticate the connection.
     

<li> How to use?   </li>
<ol>

<li> Plan your mod-pack and add all the mods you need to the 'mods' folder. </li>
<li> After the preparation, run both client and server and wait until the config files are generated.</li>   
<li> Goto client folder \.minecraft\config\jarsauth-client.toml , you can change or add any folders in '\.minecraft', which will be checked by JarsAuth.</li>
<li> <strong>Restart</strong> the client and then you can create a new singleplayer world, where you can run the command "/jarsauth gethash" to get the hash code.</li>
<li> Click the underlined hash code to copy it and then paste it in the server config (in "server"\"world"\serverconfig\jarsauth-server.toml).</li>
<li> Wait for a while and drink a cup of java, and you will be able to log into the server after forge puts the change in effect automatically.</li>

</ol>

<li> Can I use the mod in my mod-pack? </li>

Of course! Just feel free to use and report bugs to me as soon as possible.



</ol>