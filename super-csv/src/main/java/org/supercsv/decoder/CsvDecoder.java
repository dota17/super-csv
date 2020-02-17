package org.supercsv.decoder;

import org.supercsv.prefs.CsvPreference;

import java.util.List;

public interface CsvDecoder {
	List<String> decode(String input, CsvPreference csvPreference);
}
