// AppListProbe.java

package usr.router;

import usr.router.*;
import usr.applications.ApplicationHandle;
import usr.applications.RuntimeMonitoring;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.list.*;
import eu.reservoir.monitoring.core.table.*;
import eu.reservoir.monitoring.appl.datarate.*;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * A probe that talks to a Router can collects the stats
 * for each executing App.
 */
public class AppListProbe extends RouterProbe implements Probe {
    // The TableHeader for the table of stats
    TableHeader statsHeader;

    // Save table, so we only send different ones
    DefaultTable savedT = null;

    /**
     * Construct a AppListProbe
     */
    public AppListProbe(RouterController cont) {
        setController(cont);

        // set probe name
        setName(cont.getName()+".appList");
        // set data rate
        setDataRate(new EveryNSeconds(10));

        // Define the header. Has:
        // Name
        // ThreadName
        // ClassName
        // State
        statsHeader = new DefaultTableHeader()
            .add("AID", ProbeAttributeType.INTEGER)
            .add("StartTime", ProbeAttributeType.LONG)
            .add("RunTime", ProbeAttributeType.INTEGER)
            .add("State", ProbeAttributeType.STRING)
            .add("ClassName", ProbeAttributeType.STRING)
            .add("Args", ProbeAttributeType.STRING)
            .add("Name", ProbeAttributeType.STRING)
            .add("RuntimeKeys", ProbeAttributeType.LIST)
            .add("RuntimeValues", ProbeAttributeType.LIST)
        ;

        //add("ThreadName", ProbeAttributeType.STRING);


        // setup the probe attributes
        // The router name
        // The table of stats
        addProbeAttribute(new DefaultProbeAttribute(0, "RouterName", ProbeAttributeType.STRING, "name"));
        addProbeAttribute(new TableProbeAttribute(1, "Data", statsHeader));

    }

    /**
     * Collect a measurement.
     */
    public ProbeMeasurement collect() {
        //System.out.println("AppListProbe: collect()");

        // get list of apps
        Collection<ApplicationHandle> appList = getController().appList();

        if (appList == null || appList.size() == 0) {
            // no apps to report
            return null;

        } else {

            try {

                // collate measurement values
                ArrayList<ProbeValue> list = new ArrayList<ProbeValue>();

                // add router name
                list.add(new DefaultProbeValue(0, getController().getName()));

                // now allocate a table
                DefaultTable statsTable = new DefaultTable();
                statsTable.defineTable(statsHeader);

                // visit each App
                for (ApplicationHandle ah : appList) {
                    // create a row for ApplicationHandle data
                    TableRow appHRow = new DefaultTableRow();

                    // AID
                    appHRow.add(new DefaultTableValue(ah.getID()));

                    // StartTime
                    appHRow.add(new DefaultTableValue(ah.getStartTime()));

                    // RunTime
                    appHRow.add(new DefaultTableValue((int)(System.currentTimeMillis() - ah.getStartTime())));

                    // State
                    appHRow.add(new DefaultTableValue(ah.getState().toString()));

                    // ClassName
                    appHRow.add(new DefaultTableValue(ah.getApplication().getClass().getName()));

                    // Args
                    appHRow.add(new DefaultTableValue(Arrays.asList(ah.getArgs()).toString()));

                    // Name
                    appHRow.add(new DefaultTableValue(ah.getName()));

                    // check if we should get run time monitoring data
                    if (ah.getApplication() instanceof RuntimeMonitoring) {
                        // yes
                        MList keys = new DefaultMList(ProbeAttributeType.STRING);
                        MList values = new DefaultMList(ProbeAttributeType.STRING);

                        // get the data
                        Map<String, String> theMap = ((RuntimeMonitoring)ah.getApplication()).getMonitoringData();

                        // add the keys and values
                        for (Map.Entry<String, String> entry : theMap.entrySet()) {
                            keys.add(entry.getKey());
                            values.add(entry.getValue());
                        }

                        appHRow.add(new DefaultTableValue(keys));
                        appHRow.add(new DefaultTableValue(values));

                    } else {
                        // no
                        appHRow.add(new DefaultTableValue(new DefaultMList(ProbeAttributeType.STRING)));
                        appHRow.add(new DefaultTableValue(new DefaultMList(ProbeAttributeType.STRING)));

                    }

                    // add this row to the table
                    statsTable.addRow(appHRow);


                }

                list.add(new DefaultProbeValue(1, statsTable));

                // TODO: do the following as a ProbeFilter.
                // if the tables are the same, don not send a new measurement
                if (tablesEqual(savedT, statsTable)) {
                    // nothing to send
                    return null;
                } else {
                    // set the type to be: AppList
                    ProducerMeasurement lastestM = new ProducerMeasurement(this, list, "AppList");
                    savedT = statsTable;
                    return lastestM;
                }


            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Are tables equal
     */
    private boolean tablesEqual(Table t1, Table t2) {
        // check all the rows

        if (t1 == null || t2 == null) {
            return false;
        } else {
            // get sizes
            int t1Rows = t1.getRowCount();
            int t2Rows = t2.getRowCount();

            if (t1Rows != t2Rows) {
                // different size - must be different
                return false;
            } else {
                // same size - check rows
                for (int r = 0; r < t1Rows; r++) {
                    // see if the rows are equal
                    TableRow t1Row = t1.getRow(r);
                    TableRow t2Row = t2.getRow(r);

                    int size = t1Row.size();

                    for (int e = 0; e < size; e++) {
                        TableValue t1V = t1Row.get(e);
                        TableValue t2V = t2Row.get(e);

                        if (!t1V.getValue().equals(t2V.getValue())) {
                            // a value is different - therefore table must be different
                            return false;
                        }
                    }
                }

                // all the rows are the same
                return true;

            }
        }
    }

}