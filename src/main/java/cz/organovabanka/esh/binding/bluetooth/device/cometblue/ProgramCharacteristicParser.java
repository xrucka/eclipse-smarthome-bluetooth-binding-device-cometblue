package cz.organovabanka.esh.binding.bluetooth.device.cometblue;

import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.CharacteristicParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;

import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometFieldHolder;
import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometBlueDataFormats;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import static java.lang.Math.min;

public class ProgramCharacteristicParser implements CharacteristicParser {
    private static class LocalTimeInterval {
        LocalTime begin;
        LocalTime end;

        LocalTimeInterval(LocalTime begin, LocalTime end) {
            this.begin = begin;
            this.end = end;
        }

        LocalTimeInterval intersect(LocalTimeInterval b) {
            LocalTimeInterval a = this;
            // presume sorted a b, that is either b starts later or the same
            // a starts first
            if (a.end.isBefore(b.begin)) {
                // a ends before b starts
                return null;
            } else {
                // b starts either on exact time a ends, or a ends even later
                // that is, either a's end first or b's end first.
                // take maximum
                if (a.end.isBefore(b.end)) {
                    return new LocalTimeInterval(a.begin, b.end);
                } else {
                    // a envelopes b
                    return a;
                }
            }
        }

        public static class Compare implements Comparator<LocalTimeInterval> {
            @Override
            public int compare(LocalTimeInterval ti1, LocalTimeInterval ti2) {
                if (ti1.begin.isBefore(ti2.begin)) {
                    return -1;
                } else if (ti2.begin.isBefore(ti1.begin)) {
                    return 1;
                } else if (ti1.end.isBefore(ti2.end)) {
                    return -1;
                } else if (ti2.end.isBefore(ti1.end)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    public ProgramCharacteristicParser() {
        ;
    }

    private ArrayList<LocalTimeInterval> sortIntervals(ArrayList<LocalTimeInterval> entries) {
        Collections.sort(entries, new LocalTimeInterval.Compare());
        return entries;
    }
    
    private ArrayList<LocalTimeInterval> extractTimeslots(byte[] raw) {
        ArrayList<LocalTimeInterval> timeslots = new ArrayList();

        // round down to multiple of 2
        int fields = ((raw.length) / 2) * 2;
        for (int i = 0; i < fields; i += 2) {
            LocalTime startTime = CometBlueDataFormats.deserializeTime1(raw, i);
            LocalTime endTime = CometBlueDataFormats.deserializeTime1(raw, i+1);

            if (startTime == null || endTime == null) {
                continue;
            }

            if (!endTime.isAfter(startTime)) {
                continue;
            }

            timeslots.add(new LocalTimeInterval(startTime, endTime));
        }

        return timeslots;
    }

    private ArrayList<LocalTimeInterval> joinIntervals(ArrayList<LocalTimeInterval> timeslots) {
        ArrayList<LocalTimeInterval> newslots = new ArrayList();
        if (timeslots.isEmpty()) {
            return newslots;
        }

        LocalTimeInterval prevslot = timeslots.get(0);
        for (int i = 1; i < timeslots.size(); ++i)  {
            LocalTimeInterval current = timeslots.get(i);
            LocalTimeInterval intersection = prevslot.intersect(current);
            
            if (intersection != null) {
                prevslot = intersection;
                continue;
            } else {
                newslots.add(prevslot);
                prevslot = current;
            }
        }
        newslots.add(prevslot);
        return newslots;
    }

    private String stringifyEntry(LocalTimeInterval entry) {
        return entry.begin.toString() + "-" + entry.end.toString();
    }

    private void complete(ArrayList<LocalTimeInterval> timeslots) {
        while (timeslots.size() < 4) {
            timeslots.add(new LocalTimeInterval(CometBlueDataFormats.FAIL_PLACEHOLDER_T, CometBlueDataFormats.FAIL_PLACEHOLDER_T));
        }

        while (timeslots.size() > 4) {
            timeslots.remove(4);
        }
    }

    private String stringify(ArrayList<LocalTimeInterval> timeslots) {
        complete(timeslots);
        return "[" + timeslots.stream().map((e) -> stringifyEntry(e)).collect(Collectors.joining("; ")) + "]";
    }

    @Override
    public LinkedHashMap<String, FieldHolder> parse(Characteristic characteristic, byte[] raw) throws CharacteristicFormatException {
        LinkedHashMap<String, FieldHolder> holders = new LinkedHashMap();

        ArrayList<LocalTimeInterval> timeslots = extractTimeslots(raw);
        timeslots = sortIntervals(timeslots);
        timeslots = joinIntervals(timeslots);

        String strvalue = stringify(timeslots);
        Field f = characteristic.getValue().getFields().get(0);
        
        FieldHolder holder = new CometFieldHolder(f, strvalue);
        holders.put(f.getName(), holder);

        return holders;
    }

    private ArrayList<LocalTimeInterval> extractTimeslots(String dataSentence) throws CharacteristicFormatException {
        ArrayList<LocalTimeInterval> result = new ArrayList();

        dataSentence = dataSentence.trim();
        dataSentence = dataSentence.replaceAll("[\\[\\]\\s]", "");
        dataSentence = dataSentence.replaceAll("\\.", ":");
        String[] dataSentences = dataSentence.split("[,;]+");

        for (String segment : dataSentences) {
            String[] components = segment.split("-+");
            LocalTime[] times = new LocalTime[2];
            
            for (int i = 0; i < 2; ++i) {
                String fragment = (i < components.length) ? components[i] : null;
                if ((fragment == null) || ("".equals(fragment))) {
                    fragment = CometBlueDataFormats.FAIL_PLACEHOLDER_T.toString();
                }
                
                try {
                    times[i] = LocalTime.parse(fragment);
                } catch (DateTimeParseException pe) {
                    throw new CharacteristicFormatException("Unable to parse \"" + fragment + "\" as time: " + pe.getMessage(), pe);
                }
            }

            result.add(new LocalTimeInterval(times[0], times[1]));
        }

        return result;
    }

    @Override
    public byte[] serialize(Collection<FieldHolder> fieldHolders) throws CharacteristicFormatException {
        if (fieldHolders.size() != 1) {
            throw new CharacteristicFormatException("Broken field holders");
        }

        Iterator<FieldHolder> fieldIterator = fieldHolders.iterator();
        String stringval = fieldIterator.next().getString();
        ArrayList<LocalTimeInterval> timeslots = extractTimeslots(stringval);
        timeslots = sortIntervals(timeslots);
        timeslots = joinIntervals(timeslots);
        complete(timeslots);

        byte[] data = new byte[8];
        for (int i = 0; i < 4; ++i) {
            LocalTimeInterval te = timeslots.get(i);

            CometBlueDataFormats.serializeTime1(te.begin, data, 2*i + 0);
            CometBlueDataFormats.serializeTime1(te.end, data, 2*i + 1);
        }
        
        return data;
    } 

}
