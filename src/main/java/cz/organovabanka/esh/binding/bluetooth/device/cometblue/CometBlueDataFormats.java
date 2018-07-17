package cz.organovabanka.esh.binding.bluetooth.device.cometblue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.CharacteristicParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;

import cz.organovabanka.esh.binding.bluetooth.device.cometblue.CometFieldHolder;

import java.time.DateTimeException;
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
import static java.lang.Math.min;

public class CometBlueDataFormats {
    private static final Logger logger = LoggerFactory.getLogger(CometBlueDataFormats.class);

    public static final int YEAR_OFFSET = 2000;
    public static final float TEMP_OFFSET = 0;
    public static final LocalTime FAIL_PLACEHOLDER_T = LocalTime.of(0, 0);
    public static final LocalDateTime FAIL_PLACEHOLDER_DT = LocalDateTime.of(YEAR_OFFSET, 1, 1, 0, 0);

    public static LocalDateTime deserializeDateTime4(byte[] raw, int offset) {
        int hour = (int)raw[offset + 0] & 0xff;
        int day = (int)raw[offset + 1] & 0xff;
        int month = (int)raw[offset + 2] & 0xff;
        int year = YEAR_OFFSET + ((int)raw[offset + 3] & 0xff);

        try {
            return LocalDateTime.of(year, month, day, hour, 0);
        } catch (DateTimeException err) {
            logger.debug("Failed to deserialize date and time, invalid value upon input");
            return null;
        }
    }

    public static LocalDateTime deserializeDateTime5(byte[] raw, int offset) {
        int minute = (int)raw[offset + 0] & 0xff;
        LocalDateTime date = deserializeDateTime4(raw, offset + 1);

        if (date == null) {
            return null;
        }

        try {
            return date.withMinute(minute);
        } catch (DateTimeException err) {
            logger.debug("Failed to deserialize date and time, invalid value upon input");
        }
        return null;
    }

    public static void serializeDateTime4(LocalDateTime time, byte[] raw, int offset) {
        raw[offset + 0] = (byte)time.getHour();
        raw[offset + 1] = (byte)time.getDayOfMonth();
        raw[offset + 2] = (byte)time.getMonthValue();
        raw[offset + 3] = (byte)(time.getYear() - YEAR_OFFSET);
    }

    public static void serializeDateTime5(LocalDateTime time, byte[] raw, int offset) {
        raw[offset + 0] = (byte)time.getMinute();
        serializeDateTime4(time, raw, offset + 1);
    }

    public static LocalTime deserializeTime1(byte[] raw, int offset) {
        byte entry = raw[offset + 0];

        int hour = ((int)entry & 0xff) / 6;
        int minute = (((int)entry & 0xff) % 6) * 10;

        try {	
            return LocalTime.of(hour, minute);
        } catch (DateTimeException err) {
            logger.debug("Failed to deserialize and time, invalid value upon input");
        }

        return null;
    }

    public static void serializeTime1(LocalTime time, byte[] raw, int offset) {
        int hour = time.getHour();
        int minute = time.getMinute();
        byte value = (byte)((hour * 6) + (minute / 10));
        raw[offset + 0] = value;
    }

    public static float deserializeTemp1(byte[] raw, int offset) {
        int ival = (int)raw[offset + 0] & 0xff;
        float fval = ((float)ival / 2) + TEMP_OFFSET;
        return fval;
    }

    public static void serializeTemp1(float temp, byte[] raw, int offset) {
        temp -= TEMP_OFFSET;
        temp *= 2;
        int ival = (int)temp;
        raw[offset + 0] = (byte)ival;
    }



}
