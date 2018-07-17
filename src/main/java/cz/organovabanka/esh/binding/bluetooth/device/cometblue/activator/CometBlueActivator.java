package cz.organovabanka.esh.binding.bluetooth.device.cometblue.activator;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.organovabanka.esh.binding.bluetooth.device.cometblue.ProgramCharacteristicParser;
import cz.organovabanka.esh.binding.bluetooth.device.cometblue.HolidayCharacteristicParser;
import cz.organovabanka.esh.binding.bluetooth.device.cometblue.DateTimeParser;

import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.CharacteristicParser;

import java.net.URL;
import java.net.MalformedURLException;

@Component(immediate = true, name = "binding.bluetooth.device.cometblue.activator")
public class CometBlueActivator {
    private final Logger logger = LoggerFactory.getLogger(CometBlueActivator.class);

    private final String[] PROGRAM_UUIDS = { "47E9EE10", "47E9EE11", "47E9EE12", "47E9EE13", "47E9EE14", "47E9EE15", "47E9EE16" };
    private final String[] HOLIDAY_UUIDS = { "47E9EE20", "47E9EE21", "47E9EE22", "47E9EE23", "47E9EE24", "47E9EE25", "47E9EE26", "47E9EE27" };
    private final String[] DATE_UUIDS = { "47E9EE01" };

    BluetoothGattParser superParser = null;

    public CometBlueActivator() {
        ;
    }

    @Activate
    public void activate(BundleContext context) {
        // noop
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        // noop
    }

    @Reference(unbind = "unregisterBluetoothGattParser", cardinality = ReferenceCardinality.MANDATORY)
    protected void registerBluetoothObjectFactory(BluetoothGattParser superParser) {
        if (superParser == null) {
            return;
        }

        this.superParser = superParser;

        loadServices();
        registerDateTimeParser();
        registerProgramParser();
        registerHolidayParser();
    }

    protected void unregisterBluetoothGattParser(BluetoothGattParser superParser) {
        // noop
    }   

    private void loadServices() {
        URL servicesResource = this.getClass().getResource("/gatt/service/gatt_spec_registry.json");
        URL characteristicsResource = this.getClass().getResource("/gatt/characteristic/gatt_spec_registry.json");
        if (servicesResource == null || characteristicsResource == null) {
            logger.error("Failed to aquire required resources {}, {}", servicesResource, characteristicsResource);
            return;
        }         
        superParser.loadExtensionsFromCatalogResources(servicesResource, characteristicsResource);
    } 

    private void registerDateTimeParser() {
        DateTimeParser dateParser = new DateTimeParser();
        for (String dateUuid : DATE_UUIDS) {
            superParser.registerParser(dateUuid, dateParser);
        }
    }

    private void registerProgramParser() {
        ProgramCharacteristicParser programParser = new ProgramCharacteristicParser();
        for (String programUuid : PROGRAM_UUIDS) {
            superParser.registerParser(programUuid, programParser);
        }
    }

    private void registerHolidayParser() {
        HolidayCharacteristicParser holidayParser = new HolidayCharacteristicParser();
        for (String holidayUuid : HOLIDAY_UUIDS) {
            superParser.registerParser(holidayUuid, holidayParser);
        }
    }
}
