package ru.textanalysis.tawt.rn;

import lombok.extern.slf4j.Slf4j;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdkFactory;
import ru.textanalysis.tawt.ms.interfaces.InitializationModule;
import ru.textanalysis.tawt.ms.model.jmorfsdk.Form;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.textanalysis.tawt.rn.Consts.DEPTH_DEFAULT;

@Slf4j
public class RelationshipNetworks implements InitializationModule {

	private final InternalLoader internalLoader;

	private JMorfSdk jMorfSdk;

	private Map<Integer, Set<Integer>> rows = new HashMap<>();
	private Map<Integer, List<List<Integer>>> words = new HashMap<>();

	public RelationshipNetworks() {
		this(DEPTH_DEFAULT);
	}

	public RelationshipNetworks(int depth) {
		this.internalLoader = new InternalLoader(depth);
	}

	@Override
	public synchronized void init() {
		internalLoader.init();
		rows = internalLoader.getRows();
		words = internalLoader.getWords();
		jMorfSdk = JMorfSdkFactory.loadFullLibrary();
	}

	public int rowsSize() {
		return rows.size();
	}

	public int wordsSize() {
		return words.size();
	}

	public Set<List<Form>> getWords(String intForm) {
		int key = jMorfSdk.getOmoForms(intForm).get(0).getInitialForm().getInitialFormKey();
		return rows.get(words.get(key).get(0).get(0)).stream()
			.map(hash -> jMorfSdk.getOmoForms(hash))
			.collect(Collectors.toSet());
	}

	public List<List<Integer>> getWords(Integer intForm) {
		return words.get(intForm);
	}

	public boolean containsKeyWords(Integer initialFormKey) {
		return words.containsKey(initialFormKey);
	}

	public Set<Integer> getRows(Integer integer) {
		return rows.get(integer);
	}

	public boolean containsKeyRows(Integer initialFormKey) {
		return rows.containsKey(initialFormKey);
	}
}
