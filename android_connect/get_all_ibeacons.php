<?php

/*
 * Following code will list all the ibeacons
 */

// array for JSON response
$response = array();

// include db connect class
require_once __DIR__ . '/db_connect.php';

// connecting to db
$db = new DB_CONNECT();

// get all ibeacons from ibeacons table
$result = mysql_query("SELECT *FROM ibeacons") or die(mysql_error());

// check for empty result
if (mysql_num_rows($result) > 0) {
    // looping through all results
    // ibeacons node
    $response["ibeacons"] = array();

    while ($row = mysql_fetch_array($result)) {
        // temp user array
        $ibeacon = array();
        $ibeacon["pid"] = $row["pid"];
        $ibeacon["name"] = $row["name"];
        $ibeacon["colour"] = $row["colour"];
        $ibeacon["major"] = $row["major"];
        $ibeacon["minor"] = $row["minor"];

        // push single ibeacon into final response array
        array_push($response["ibeacons"], $ibeacon);
    }
    // success
    $response["success"] = 1;

    // echoing JSON response
    echo json_encode($response);
} else {
    // no ibeacons found
    $response["success"] = 0;
    $response["message"] = "No ibeacons found";

    // echo no users JSON
    echo json_encode($response);
}
?>