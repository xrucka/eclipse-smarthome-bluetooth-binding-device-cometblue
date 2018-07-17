package cz.organovabanka.esh.binding.bluetooth.device.cometblue;

import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.CharacteristicParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;

import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometFieldHolder;
import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometBlueDataFormats;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.lang.Math.min;

public class HolidayCharacteristicParser implements CharacteristicParser {
    public HolidayCharacteristicParser() {
        ;
    }

    private String stringify(LocalDateTime startTime, LocalDateTime endTime, float temp) {
        String start = startTime.toString();
        String end = endTime.toString();

        return "[ " + start + " ; " + end + "] = " + Float.toString(temp) + " °C";
    }

    private void extractEntry(String strval, byte[] raw) throws CharacteristicFormatException {
        int rangeDelimiter = 0;
        int valueDelimiter = 0;

        String startstr = null;
        String endstr = null;
        String tempstr = null;

        strval = strval.replaceAll("\\[|\\]", "");

        try {
            rangeDelimiter = strval.indexOf(";");
            valueDelimiter = strval.indexOf("=", rangeDelimiter);

            startstr = strval.substring(0, rangeDelimiter).trim();
            endstr = strval.substring(rangeDelimiter + 1, valueDelimiter).trim();
            tempstr = strval.substring(valueDelimiter + 1).trim();
        } catch (IndexOutOfBoundsException pe) {
            throw new CharacteristicFormatException("Unable to parse \"" + strval + "\", invalid format", pe);
        }

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        float temp = 18.0f;

        try {
            startTime = LocalDateTime.parse(startstr);
        } catch (DateTimeParseException pe) {
            throw new CharacteristicFormatException("Unable to parse \"" + startstr + "\" as time: " + pe.getMessage(), pe);
        }
 
        try {
            endTime = LocalDateTime.parse(endstr);
        } catch (DateTimeParseException pe) {
            throw new CharacteristicFormatException("Unable to parse \"" + endstr + "\" as time: " + pe.getMessage(), pe);
        }
    
        Pattern p = Pattern.compile("^[0-9]*([.][0-9]*)?([eE][0-9]+)?");
        Matcher m = p.matcher(tempstr);
        if (!m.find()) {
            throw new CharacteristicFormatException("Unable to parse \"" + tempstr + "\" as temperature in degree Celsius");
        }

        String ftemp = m.group();
        try {
            temp = Float.valueOf(ftemp);
        } catch (NumberFormatException pe) {
            throw new CharacteristicFormatException("Unable to parse \"" + tempstr + "\" as temperature: " + pe.getMessage(), pe);
        }

        CometBlueDataFormats.serializeDateTime4(startTime, raw, 0);
        CometBlueDataFormats.serializeDateTime4(endTime, raw, 4);
        CometBlueDataFormats.serializeTemp1(temp, raw, 8);
    }

    @Override
    public LinkedHashMap<String, FieldHolder> parse(Characteristic characteristic, byte[] raw) throws CharacteristicFormatException {
        LinkedHashMap<String, FieldHolder> holders = new LinkedHashMap();

        LocalDateTime startTime = CometBlueDataFormats.deserializeDateTime4(raw, 0);
        if (startTime == null) {
            startTime = CometBlueDataFormats.FAIL_PLACEHOLDER_DT;
        }

        LocalDateTime endTime = CometBlueDataFormats.deserializeDateTime4(raw, 4);
        if (endTime == null) {
            endTime = CometBlueDataFormats.FAIL_PLACEHOLDER_DT;
        }

        float temp = CometBlueDataFormats.deserializeTemp1(raw, 8);

        String strvalue = stringify(startTime, endTime, temp);
        Field f = characteristic.getValue().getFields().get(0);
        
        FieldHolder holder = new CometFieldHolder(f, strvalue);
        holders.put(f.getName(), holder);

        return holders;
    }

    @Override
    public byte[] serialize(Collection<FieldHolder> fieldHolders) throws CharacteristicFormatException {
        if (fieldHolders.size() != 1) {
            throw new CharacteristicFormatException("Broken field holders");
        }

        Iterator<FieldHolder> fieldIterator = fieldHolders.iterator();
        String stringval = fieldIterator.next().getString();

        byte[] data = new byte[9];
        extractEntry(stringval, data);
        return data;
    } 

}
