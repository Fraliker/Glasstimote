<?php

/*
 * Following code will get single ibeacon details
 * A ibeacon is identified by ibeacon minor (minor)
 */

// array for JSON response
$response = array();

// include db connect class
require_once __DIR__ . '/db_connect.php';

// connecting to db
$db = new DB_CONNECT();

// check for post data
if (isset($_GET["minor"])) {
    $minor = $_GET['minor'];

    // get a ibeacon from ibeacons table
    $result = mysql_query("SELECT *FROM ibeacons WHERE minor = $minor");

    if (!empty($result)) {
        // check for empty result
        if (mysql_num_rows($result) > 0) {

            $result = mysql_fetch_array($result);

            $ibeacon = array();

            $ibeacon["pid"] = $result["pid"];
            $ibeacon["name"] = $result["name"];
            $ibeacon["colour"] = $result["colour"];
            $ibeacon["address"] = $result["address"];
            $ibeacon["major"] = $result["major"];
            $ibeacon["minor"] = $result["minor"];
            $ibeacon["location_image_url"] = $result["location_image_url"];
            $ibeacon["location_name"] = $result["location_name"];
            $ibeacon["location_info"] = $result["location_info"];

            // success
            $response["success"] = 1;

            // user node
            $response["ibeacon"] = array();

            array_push($response["ibeacon"], $ibeacon);

            // echoing JSON response
            echo json_encode($response);
        } else {
            // no ibeacon found
            $response["success"] = 0;
            $response["message"] = "No ibeacon found";

            // echo no users JSON
            echo json_encode($response);
        }
    } else {
        // no ibeacon found
        $response["success"] = 0;
        $response["message"] = "No ibeacon found";

        // echo no users JSON
        echo json_encode($response);
    }
} else {
    // required field is missing
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing";

    // echoing JSON response
    echo json_encode($response);
}
?>