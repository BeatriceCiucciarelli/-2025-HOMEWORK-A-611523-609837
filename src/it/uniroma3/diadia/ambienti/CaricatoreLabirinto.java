package it.uniroma3.diadia.ambienti;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import it.uniroma3.diadia.FormatoFileNonValidoException;
import it.uniroma3.diadia.ambienti.Labirinto.LabirintoBuilder;



public class CaricatoreLabirinto {

	/* prefisso di una singola riga di testo contenente tutti i nomi delle stanze */
	private static final String STANZE = "Stanze:";             

	/* prefisso di una singola riga contenente il nome della stanza iniziale */
	private static final String STANZA_INIZIALE_MARKER = "Inizio:";    

	/* prefisso della riga contenente il nome stanza vincente */
	private static final String STANZA_VINCENTE_MARKER = "Vincente:";  

	/* prefisso della riga contenente le specifiche degli attrezzi da collocare nel formato <nomeAttrezzo> <peso> <nomeStanza> */
	private static final String ATTREZZI = "Attrezzi:";

	/* prefisso della riga contenente le specifiche dei collegamenti tra stanza nel formato <nomeStanzaDa> <direzione> <nomeStanzaA> */
	private static final String USCITE = "Uscite:";

	private static final String MAGICHE = "Magiche:";
	private static final String ESTREMI = "Estremi:";
	private static final String BUIE = "Buie:";
	private static final String CHIUSE = "Chiuse:";
	private static final String PERSONAGGI = "Personaggi:";


	/*
	 *  Esempio di un possibile file di specifica di un labirinto (vedi POO-26-eccezioni-file.pdf)

		Stanze: biblioteca, N10, N11
		Inizio: N10
		Vincente: N11
		Attrezzi: martello 10 biblioteca, pinza 2 N10
		Uscite: biblioteca nord N10, biblioteca sud N11

	 */

	private BufferedReader reader;
	private Set<String> stanze;
	private int numeroLinea;
	private LabirintoBuilder builder;

	public CaricatoreLabirinto(String nomeFile) throws FileNotFoundException {
		this(new FileReader(nomeFile));
	}

	public CaricatoreLabirinto(Reader reader) {
		this.stanze = new HashSet<String>();
		this.numeroLinea = 0;
		this.reader = new BufferedReader(reader);
		this.builder = Labirinto.newBuilder();
	}

	public void carica() {
		try {
			this.leggiStanze();
			this.leggiStanzeMagiche();
			this.leggiInizialeEvincente();
			this.leggiAttrezzi();
			this.leggiUscite();
			this.leggiStanzeBuie();       // NUOVO
			this.leggiStanzeChiuse();    //NUOVO
			this.leggiPersonaggi();      //NUOVO

		} catch (FormatoFileNonValidoException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

	}

	private String leggiRiga(BufferedReader reader) throws FormatoFileNonValidoException {
		try {
			this.numeroLinea++;
			String riga = reader.readLine();
			//System.err.println("Letta riga "+ this.numeroLinea + ": "+ riga);
			return riga;
		} catch (IOException e) {
			throw new FormatoFileNonValidoException("Problemi lettura file [" + this.numeroLinea + "]");
		}
	}

	private void leggiInizialeEvincente() throws FormatoFileNonValidoException {
		String nomeStanzaIniziale = null;
		nomeStanzaIniziale = this.leggiRiga(reader);
		this.stanze.add(nomeStanzaIniziale);
		this.builder.addStanzaIniziale(nomeStanzaIniziale);
		String nomeStanzaVincente = this.leggiRiga(reader);
		this.stanze.add(nomeStanzaVincente);
		this.builder.addStanzaVincente(nomeStanzaVincente);
		String token = this.leggiRiga(reader);
		if (!token.equals(ATTREZZI))
			throw new FormatoFileNonValidoException("Formato file non valido [" + this.numeroLinea + "]:" +ATTREZZI +" non trovato");		
	}

	private void leggiStanze() throws FormatoFileNonValidoException  {
		String nomeStanza = null;
		nomeStanza = this.leggiRiga(reader);
		if (!nomeStanza.equals(STANZE))
			throw new FormatoFileNonValidoException("Formato file non valido [" + this.numeroLinea + "]"+": "+STANZE +" non trovato");
		nomeStanza = this.leggiRiga(reader);
		while (!nomeStanza.equals(MAGICHE)) {
			if (nomeStanza == null)
				throw new FormatoFileNonValidoException("Termine inaspettata del file [" + this.numeroLinea + "].");
			this.builder.addStanza(nomeStanza);
			this.stanze.add(nomeStanza);
			nomeStanza = this.leggiRiga(reader);
		}
	}

	private void leggiStanzeMagiche() throws FormatoFileNonValidoException  {
		String nomeStanza = null;
		nomeStanza = this.leggiRiga(reader);
		while (!nomeStanza.equals(ESTREMI)) {
			if (nomeStanza == null)
				throw new FormatoFileNonValidoException("Termine inaspettata del file [" + this.numeroLinea + "].");
			this.builder.addStanzaMagica(nomeStanza);
			this.stanze.add(nomeStanza);
			nomeStanza = this.leggiRiga(reader);
		}
	}

	private void posaAttrezzo(String nomeStanza, String nomeAttrezzo, int peso) {
		this.builder.addAttrezzo(nomeStanza, nomeAttrezzo, peso);
	}
	

	private void leggiAttrezzi() throws FormatoFileNonValidoException {
		String nomeAttrezzo = null;
		String pesoAttrezzo = null;
		String nomeStanza = null; 
		String definizioneAttrezzo = this.leggiRiga(reader);

		while (!definizioneAttrezzo.equals(USCITE)) {
			int peso;
			Scanner scannerLinea = new Scanner(definizioneAttrezzo);
			nomeAttrezzo = scannerLinea.next();
			if (nomeAttrezzo == null)
				throw new FormatoFileNonValidoException("Termine inaspettata del file [" + this.numeroLinea + "].");
			pesoAttrezzo = scannerLinea.next();
			try {
				peso = Integer.parseInt(pesoAttrezzo);
			}
			catch (NumberFormatException e) {
				throw new FormatoFileNonValidoException("Peso attrezzo "+nomeAttrezzo+" non valido [" + this.numeroLinea + "].");
			}
			nomeStanza = scannerLinea.next();

			if (!stanzaValida(nomeStanza))
				throw new FormatoFileNonValidoException("Definizione attrezzo "+ nomeAttrezzo+" errata [" + this.numeroLinea + "]" +": stanza" +nomeStanza+" inesistente");
			posaAttrezzo(nomeStanza, nomeAttrezzo, peso);
			definizioneAttrezzo = this.leggiRiga(reader);
		}
	}

	private boolean stanzaValida(String nomeStanza) {
		return this.stanze.contains(nomeStanza);
	}

	private void impostaUscita(String nomeUscita, String nomeStanzaPartenza, String nomeStanzaDestinazione) {
		this.builder.addAdiacenza(nomeStanzaPartenza, nomeStanzaDestinazione, nomeUscita);
	}

	private void leggiUscite()
			throws FormatoFileNonValidoException {
		String nomeStanzaPartenza = null;
		String nomeUscita = null;
		String nomeStanzaDestinazione = null;
		String datiUscita = this.leggiRiga(reader);
		while (datiUscita != null) {
			try (Scanner scannerDiLinea = new Scanner(datiUscita)) {
				while (scannerDiLinea.hasNext()) {
					nomeStanzaPartenza = scannerDiLinea.next();
					nomeUscita = scannerDiLinea.next();
					nomeStanzaDestinazione = scannerDiLinea.next();
					if (!stanzaValida(nomeStanzaPartenza))
						throw new FormatoFileNonValidoException("Definizione errata uscita [" + this.numeroLinea + "]" + nomeUscita);
					if (!stanzaValida(nomeStanzaDestinazione))
						throw new FormatoFileNonValidoException("Definizione errata uscita [" + this.numeroLinea + "]" + nomeUscita);
					impostaUscita(nomeUscita, nomeStanzaPartenza, nomeStanzaDestinazione);
				}
			}			
			datiUscita = this.leggiRiga(reader);
		} 
	}


	private void leggiStanzeBuie() throws FormatoFileNonValidoException {
		String riga = this.leggiRiga(reader);
		while (!riga.equals(CHIUSE)) {
			Scanner scanner = new Scanner(riga);
			String nome = scanner.next();
			String attrezzoLuce = scanner.next();
			this.builder.addStanzaBuia(nome, attrezzoLuce);
			this.stanze.add(nome);
			riga = this.leggiRiga(reader);
		}
	}

	private void leggiStanzeChiuse() throws FormatoFileNonValidoException {
		String riga = this.leggiRiga(reader);
		while (!riga.equals(STANZA_INIZIALE_MARKER)) {
			Scanner scanner = new Scanner(riga);
			String nome = scanner.next();
			String direzione = scanner.next();
			String chiave = scanner.next();
			this.builder.addStanzaBloccata(nome, direzione, chiave);
			this.stanze.add(nome);
			riga = this.leggiRiga(reader);
		}
	}


	private void leggiPersonaggi() throws FormatoFileNonValidoException {
		String riga = this.leggiRiga(reader);
		while (riga != null && !riga.equals(USCITE)) {
			Scanner scanner = new Scanner(riga);
			String tipo = scanner.next();
			String nome = scanner.next();
			String presentazione = scanner.next(); // puoi gestire virgolette se vuoi
			String stanza = scanner.next();

			switch (tipo.toLowerCase()) {
			case "mago":
				String attrezzo = scanner.next();
				int peso = scanner.nextInt();
				this.builder.addMago(nome, presentazione, stanza, attrezzo, peso);
				break;
			case "cane":
				String ciboPreferito = scanner.next();
				String attrezzoCane = scanner.next();
				int pesoCane = scanner.nextInt();
				this.builder.addCane(nome, presentazione, stanza, ciboPreferito, attrezzoCane);
				break;
			case "strega":
				this.builder.addStrega(nome, presentazione, stanza);
				break;
			default:
				throw new FormatoFileNonValidoException("Tipo di personaggio sconosciuto: " + tipo);
			}

			riga = this.leggiRiga(reader);
		}
	}


	public Labirinto getLabirinto() {
		return this.builder.getLabirinto();
	}
}
