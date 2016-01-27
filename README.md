Siddhi Complex Event Processing Engine 
======================================
This is an another brach of the original [wso2/siddhi](https://github.com/wso2/siddhi)

# To install this Siddhi library into your local maven repository
1. ```git clone http://gitlab.obzen.com:8085/lab/siddhi.git```
2. ```cd siddhi```
3. ```mvn install -DskipTests```

# Siddhi CEP Query Language Guide
Please refer to [SiddhiQL Guide 3.0](https://docs.wso2.com/display/CEP400/SiddhiQL+Guide+3.0)

# Fix log
2016.01.25 <br>
Reason: NullPointerException when null value comes in partition key <br>
File: <br>
modules/siddhi-core/src/main/java/org/wso2/siddhi/core/partition/executor/ValuePartitionExecutor.java <br>
Before:

    public String execute(ComplexEvent event) {
        return expressionExenutor.execute(event).toString();
    }

After: 

    public String execute(ComplexEvent event) {
        try {
           return expressionExecutor.execute(event).toString();
        } catch(NullPointerException e) {
           return null;
        }   
    }
