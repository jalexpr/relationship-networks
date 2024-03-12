package ru.textanalysis.tawt.rn;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdkFactory;
import ru.textanalysis.tawt.ms.model.jmorfsdk.Form;
import ru.textanalysis.tawt.ms.model.jmorfsdk.TypeForms;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class InternalLoader {

	private final String path = System.getProperty("java.io.tmpdir");

	private final File dictionary;
	private final int depth;

	private final Map<Integer, Set<Integer>> rows = new HashMap<>();
	private final Map<Integer, List<List<Integer>>> words = new HashMap<>();

	private JMorfSdk jMorfSdk;

	public InternalLoader(int depth) {
		this.depth = depth;
		String name = "Babenko_DictionaryOfSynonyms.txt";
		URL url = getClass().getClassLoader().getResource(name);
		if (url == null) {
			log.warn("Not found Dictionary: " + name);
			dictionary = null;
		} else {
			dictionary = new File(name);
			jMorfSdk = JMorfSdkFactory.loadFullLibrary();
		}
	}

	public void init() {
		if (dictionary != null) {
			extracted();
		}
	}

	public Map<Integer, List<List<Integer>>> getWords() {
		return words;
	}

	public Map<Integer, Set<Integer>> getRows() {
		return rows;
	}

	private void extracted() {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(dictionary.getName());
		if (inputStream == null) {
			log.warn("cannot load dictionary for relationship-networks because the dictionary was not found");
		} else {
			try (InputStreamReader fr = new InputStreamReader(inputStream)) {
				BufferedReader reader = new BufferedReader(fr);
				String line = reader.readLine();
				while (line != null) {
					line = cache(reader, line);
				}
			} catch (IOException ex) {
				log.warn("cannot load dictionary for relationship-networks", ex);
			}

			for (int countWord = 0; countWord < depth; countWord++) {
				cache(countWord);
			}
		}
	}

	private void cache(int depth) {
		int id = words.size();
		List<Map.Entry<Integer, List<List<Integer>>>> allWords = new ArrayList<>(words.entrySet());
		for (Map.Entry<Integer, List<List<Integer>>> wordEnter : allWords) {
			Integer wordId = wordEnter.getKey();
			List<List<Integer>> sense = wordEnter.getValue();
			for (List<Integer> depthRows : sense) {
				Integer idRow = depthRows.get(depth);
				Set<Integer> preWord = depthRows.stream()
					.map(rows::get)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet());
				Set<Integer> newRow = rows.get(idRow).stream()
					.filter(word -> !Objects.equals(word, wordId))
					.map(words::get)
					.flatMap(senseWord -> senseWord.stream().map(integers -> integers.get(depth)))
					.map(rows::get)
					.flatMap(Collection::stream)
					.filter(word -> !Objects.equals(word, wordId) && !preWord.contains(word))
					.collect(Collectors.toSet());
				rows.put(++id, newRow);
				depthRows.add(id);
			}
		}

		System.out.println(depth + " rows = " + rows.size());
		System.out.println(depth + " words = " + words.size());
	}

	private String cache(BufferedReader reader, String line) throws IOException {
		boolean word = false;
		int id = words.size() + 1;
		do {
			if (Pattern.matches("[а-я]{2,}.+", line)) {
				if (word) {
					return line;
				} else {
					Set<Integer> str = Arrays.stream(line.toLowerCase(Locale.ROOT).split("[^а-я .]+"))
						.map(String::trim)
						.filter(StringUtils::isNotBlank)
						.map(jMorfSdk::getOmoForms)
						.filter(forms -> !forms.isEmpty())
						.filter(forms -> forms.get(0).getTypeForm() != TypeForms.UNFAMILIAR)
						.map(forms -> {
							Form form = forms.get(0);
//                                initForms.putIfAbsent(form.getInitialFormKey(), form);
							return form.getInitialFormKey();
						})
						.collect(Collectors.toSet());
					if (str.isEmpty()) {
						continue;
					} else {
						word = true;
					}
					rows.putIfAbsent(++id, str);
					for (Integer s : str) {
						List<Integer> l = new LinkedList<>();
						l.add(id);
						if (words.containsKey(s)) {
							words.get(s).add(l);
						} else {
							List<List<Integer>> l2 = new LinkedList<>();
							l2.add(l);
							words.put(s, l2);
						}
					}
				}
			}
		} while ((line = reader.readLine()) != null);
		return null;
	}
}
