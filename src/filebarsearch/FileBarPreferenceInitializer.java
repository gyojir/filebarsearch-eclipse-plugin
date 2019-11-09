package filebarsearch;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;

public class FileBarPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(PreferenceConstants.COLOR_REFERENCE 					, StringConverter.asString(ColorConstants.REFERENCE));
        store.setDefault(PreferenceConstants.COLOR_DECLARATION 				, StringConverter.asString(ColorConstants.DECLARATION));
        store.setDefault(PreferenceConstants.COLOR_VARIABLE_TYPE_DECLARATION	, StringConverter.asString(ColorConstants.VARIABLE_TYPE_DECLARATION));
        store.setDefault(PreferenceConstants.COLOR_SUPERCLASS_DECLARATION 	, StringConverter.asString(ColorConstants.SUPERCLASS_DECLARATION));
        store.setDefault(PreferenceConstants.COLOR_INTERFACE_DECLARATION 	, StringConverter.asString(ColorConstants.INTERFACE_DECLARATION));
        store.setDefault(PreferenceConstants.COLOR_SUBCLASS_DECLARATION 		, StringConverter.asString(ColorConstants.SUBCLASS_DECLARATION));
        store.setDefault(PreferenceConstants.COLOR_BROTHERCLASS_DECLARATION 	, StringConverter.asString(ColorConstants.BROTHERCLASS_DECLARATION));
        store.setDefault(PreferenceConstants.COLOR_OVERLOAD_METHOD 			, StringConverter.asString(ColorConstants.OVERLOAD_METHOD));
        store.setDefault(PreferenceConstants.COLOR_INTERFACE_METHOD 			, StringConverter.asString(ColorConstants.INTERFACE_METHOD));
        store.setDefault(PreferenceConstants.COLOR_OVERRIDE_METHOD 			, StringConverter.asString(ColorConstants.OVERRIDE_METHOD));
        store.setDefault(PreferenceConstants.COLOR_OVERRIDED_METHOD 			, StringConverter.asString(ColorConstants.OVERRIDED_METHOD));
        store.setDefault(PreferenceConstants.WEIGHT_REFERENCE, 1);
        store.setDefault(PreferenceConstants.WEIGHT_DECLARATION, 1000);
        store.setDefault(PreferenceConstants.WEIGHT_VARIABLE_TYPE_DECLARATION, 100);
        store.setDefault(PreferenceConstants.WEIGHT_SUPERCLASS_DECLARATION, 100);
        store.setDefault(PreferenceConstants.WEIGHT_SUBCLASS_DECLARATION, 100);
        store.setDefault(PreferenceConstants.WEIGHT_INTERFACE_DECLARATION, 100);
        store.setDefault(PreferenceConstants.WEIGHT_BROTHERCLASS_DECLARATION, 10);
        store.setDefault(PreferenceConstants.WEIGHT_OVERLOAD_METHOD, 100);
        store.setDefault(PreferenceConstants.WEIGHT_INTERFACE_METHOD, 100);
        store.setDefault(PreferenceConstants.WEIGHT_OVERRIDE_METHOD, 100);
        store.setDefault(PreferenceConstants.WEIGHT_OVERRIDED_METHOD, 100);
        //store.setDefault(PreferenceConstants.WEIGHT_INTERNAL_INVOCATION, 1);
	}

}
