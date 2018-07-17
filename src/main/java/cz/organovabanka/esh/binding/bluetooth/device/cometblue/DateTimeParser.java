package cz.organovabanka.esh.binding.bluetooth.device.cometblue;

import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.CharacteristicParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;

import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometFieldHolder;
import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometBlueDataFormats;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class DateTimeParser implements CharacteristicParser {

    public DateTimeParser() {
        ;
    }

    @Override
    public LinkedHashMap<String, FieldHolder> parse(Characteristic characteristic, byte[] raw) throws CharacteristicFormatException {
        LocalDateTime time = CometBlueDataFormats.deserializeDateTime5(raw, 0);
        LinkedHashMap<String, FieldHolder> holders = new LinkedHashMap();

        Field f = characteristic.getValue().getFields().get(0);
        FieldHolder holder = new CometFieldHolder(f, time);
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
        LocalDateTime time = null;
        try {
            time = LocalDateTime.parse(stringval);
        } catch (DateTimeParseException pe) {
            throw new CharacteristicFormatException("Unable to parse \"" +stringval + "\" as time: " + pe.getMessage(), pe);
        }

        byte[] raw = new byte[5];
	CometBlueDataFormats.serializeDateTime5(time, raw, 0);

        return raw;
    } 
}
