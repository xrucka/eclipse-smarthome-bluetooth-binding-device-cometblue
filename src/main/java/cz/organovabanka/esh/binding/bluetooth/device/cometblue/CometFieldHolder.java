package cz.organovabanka.esh.binding.bluetooth.device.cometblue;

import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Field;

class CometFieldHolder extends FieldHolder {
    CometFieldHolder(Field field, Object value) {
	super(field, value);
    }
}
