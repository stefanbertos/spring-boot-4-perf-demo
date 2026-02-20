If user wants a detailed diff make sure to extract the zip file and perform diff of each files and provide the list of different files, and diff content of everything, or would be diffing in prometheus, grafana a better idea? and just display that?

Infra Profiles dialog - f.e. for Log Levels when you click add it should add you the default key with com.example
same for kafka topic the key should we list of all topic
kubernetes replicas the key shoudl be a list of all deployments  
then the whole profile should be part of main form, where there would be like one list of infra profile, with button to edit, create new
test scenatio should also not have the panel, but it should be a list in main form with edit,new buttons and within the scenario i can add particular test case with header, content etc.
remove the message content field, and load test case, this will come from the scenario.
As part of scenario you want to be able to set the percenatage of how many test cases from the total scenario would be send for particular test case
so remove the message count from main form and keep in on the scenario level.
Header is the first line inside the message and has usually fixed size columns with various columns so make sure it is configurable.f.e 3 char is type, then 22 bytes is transactionid and so on, user will specify the field names and sizes and values as well which will be the same for one testcase
