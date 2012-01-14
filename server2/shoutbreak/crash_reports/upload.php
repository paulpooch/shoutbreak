<?php
    $filename = isset($_POST['filename']) ? $_POST['filename'] : "";
    $message = isset($_POST['stacktrace']) ? $_POST['stacktrace'] : "";
    if (!ereg('^[-a-zA-Z0-9_. ]+$', $filename) || $message == ""){
        die("This script is used to log debug data. Please send the "
                . "logging message and a filename as POST variables.");
    }
    file_put_contents($filename, $message . "\n", FILE_APPEND);
?>