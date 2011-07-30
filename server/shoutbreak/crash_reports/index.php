<?php
    $myDirectory = opendir(".");
    while($entryName = readdir($myDirectory)) {
        $dirArray[] = $entryName;
    }
    closedir($myDirectory);
    $indexCount = count($dirArray);
    sort($dirArray);
    print("<TABLE border=1 cellpadding=5 cellspacing=0 \n");
    print("<TR><TH>Filename</TH><TH>Filetype</th><th>Filesize</TH></TR>\n");
    for($index=0; $index < $indexCount; $index++) {
        if ((substr("$dirArray[$index]", 0, 1) != ".") 
                && (strrpos("$dirArray[$index]", ".stacktrace") != false)){ 
            print("<TR><TD>");
            print("<a href=\"$dirArray[$index]\">$dirArray[$index]</a>");
            print("</TD><TD>");
            print(filetype($dirArray[$index]));
            print("</TD><TD>");
            print(filesize($dirArray[$index]));
            print("</TD></TR>\n");
        }
    }
    print("</TABLE>\n");
?>