## Purposes

1. Verify the returned values from Clicks keyboards after sending specific
   commands
2. Run firmware upgrade process to ensure Clicks keyboard firmware upgrade
   function works correctly

## Features

2 main featurs are represented/operated in the correpsonding tabs in the app:
1. Command 
  * Run a command selected from dropdown list to execute
  * Show expected and actual result for comparison
  * Select different command table to execute in case default commands are changed

2. Firmware

   Not defined yet. Show keyboard interface info as debug info

## Command tab 

Introduce the functions supported in tab 'Command'

#### Select and run the command

1. Select a specific action from the dropdown list (default value ‘Please select')
2. App would send the corresponding command to connected Clicks keyboard
3. Once received the returned value, app would print out:
   - Command sent
   - Actual response
   - Expected response
   User can compare the actual/expected response manually to verify if the execution is passed

Please note:
1. For commands setting random values (set brightness, e.g.), default values are set in advance to facilitate the comparison of returned values from get commands (get brightness, e.g.) 
2. For commands setting several fixed values (set currency symbol, e.g.):
   - Corresponding set commands are generated
   - Only one get command is created. User need to compare the matching values separately after running set command

#### Switch to new command table

In case the default command table is changed/updated, User can load new command table to the device with following XML format by clicking button 'NEW COMMANDS':
```
<?xml version="1.0" encoding="utf-8"?>
<commands>
    <command>
        <desc>Set brightness (Flash)</desc> <!-- Description -->
        <cmd>0x03 0x81 0x80</cmd>           <!-- Executed command -->
        <resp>0x04 0x02 0x81 0x00</resp>    <!-- Expected response -->
    </command>
    <command>
        <desc>Set brightness (RAM)</desc>
        <cmd>0x03 0x82 0x80</cmd>
        <resp>0x04 0x02 0x82 0x00</resp>
    </command>
    <command>
        <desc>Turn LED on</desc>
        <cmd>0x04 0x83 0x04 0x04</cmd>
        <resp>0x05 0x02 0x83 0x00 0x04</resp>
    </command>
</commands>
```
App would generate the new dropdown list with new sending commands and expected results

#### Run a test plan 

By loading a XML file with following format, app would run the commands sequentially/automatically with PASS/FAIL result by comparing actual/exepected response from device:
```
<?xml version="1.0" encoding="utf-8"?>
<testplan>
    <command>
        <desc>Set brightness (Flash)</desc> <!-- Description -->
        <cmd>0x03 0x81 0x80</cmd>           <!-- Executed command -->
        <resp>0x04 0x02 0x81 0x00</resp>    <!-- Expected response -->
    </command>
    <command>
        <desc>Set brightness (RAM)</desc>
        <cmd>0x03 0x82 0x80</cmd>
        <resp>0x04 0x02 0x82 0x00</resp>
    </command>
    <command>
        <desc>Turn LED on</desc>
        <cmd>0x04 0x83 0x04 0x04</cmd>
        <resp>0x05 0x02 0x83 0x00 0x04</resp>
    </command>
</testplan>
```

## Firmware tab

Not implemented yet. At present it shows enumerated interfaces/endpoints information as debug values

## TODO

1. Implement features in tab ‘Firmware'
2. Fully compare the actual responses and expected responses referred from vendor’s documentation of all available commands
