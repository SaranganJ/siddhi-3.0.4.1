Siddhi Complex Event Processing Engine 
======================================
This is an another brach of the original [wso2/siddhi](https://github.com/wso2/siddhi)

Fix log
-----------------
2016.01.25 <br>
Reason: NullPointerException when null value in partition key <br>
File: <br>
modules/siddhi-core/src/main/java/org/wso2/siddhi/core/partition/executor/ValuePartitionExecutor.java <br>
Before:

    public String execute(ComplexEvent event) {
        return expressionExenutor.execute(event).toString();
    }

After: 

    public String execute(ComplexEvent event) {
        String result = null;
        try {
           result =  expressionExecutor.execute(event).toString();
        } catch(NullPointerException e) {
            result = null;
        }   
        return result;
    }
