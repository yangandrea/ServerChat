package com.example;

// Server.java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class App {
    private Map<PrintWriter, String> clientMap = new HashMap<>();

    public static void main(String[] args) {
        new App().avvia();
    }

    private void avvia() {
        try (ServerSocket socket = new ServerSocket(52100)) {
            System.out.println("Server avviato");

            while (true) {
                Socket client = socket.accept();
                System.out.println("Client connesso");

                // Richiesta e salvataggio dell'username
              BufferedReader inDalClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter scriveAlClient = new PrintWriter(client.getOutputStream(), true);
                scriveAlClient.println("Inserisci il tuo username:");
                boolean controllo = true;
                do{
                String username = inDalClient.readLine();
                if(clientMap.containsValue(username)){
                    scriveAlClient.println("Username già in uso, riprova:");
                }
                else{
                    clientMap.put(scriveAlClient, username);
                    controllo = false;
                }
                } while (controllo);
                inviaListaUtentitBroadcast();

                Gestore gestore = new Gestore(client, scriveAlClient, inDalClient);
                new Thread(gestore).start();
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Gestore implements Runnable {
        private Socket client;
        private PrintWriter scriveAlClient;
        private BufferedReader inDalClient;
    
        public Gestore(Socket client, PrintWriter scriveAlClient, BufferedReader inDalClient) {
            this.client = client;
            this.scriveAlClient = scriveAlClient;
            this.inDalClient = inDalClient;
        }
    
        @Override
        public void run() {
            try {
                boolean controllo = true; 
                while (controllo) {
                    String messaggio = inDalClient.readLine();
                    System.out.println(clientMap.get(scriveAlClient) + ": " + messaggio);
                    // chiusura della connesione quando un client digita 'Exit'
                    if (messaggio.equals("Exit")) {
                        controllo = false;
                    }
                    analizzaMessaggio(clientMap.get(scriveAlClient), messaggio);
                }
            } 
            catch (IOException e) {
                e.printStackTrace();
            } 
            finally {

                clientMap.remove(scriveAlClient);

                // Se rimane solo un client invia un messaggio di aspettare, altrimenti la lista dei client connessi
                if(clientMap.size() == 1){
                    controlloNumeroDeiClient();
                }
                else {
                    inviaListaUtentitBroadcast();
                }
                
                // Chiude il BufferedReader e il Socket quando il client si disconnette
                try {
                    inDalClient.close();
                    client.close();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void analizzaMessaggio(String mittente, String messaggio) {
            // Verifica se il numero di client è sufficiente prima di inviare il messaggio
            if (clientMap.size() > 1) {

                // Verifica se il messaggio inizia con "@username" per inviarlo a un utente specifico
                if (messaggio.startsWith("@")) {
                    int spaceIndex = messaggio.indexOf(' ');
                    if (spaceIndex != -1) {
                        String username = messaggio.substring(1, spaceIndex);
                        String mesasggioPrivato = messaggio.substring(spaceIndex + 1);

                        // Invia il messaggio privatamente al destinatario se esiste
                        inviaMessaggioPrivato(mittente, username, mesasggioPrivato);
                    } 
                    else {
                        // Se il messaggio è malformato, avvisa il mittente
                        scriveAlClient.println("Formato invalido. Usa: @username message");
                    }
                } 
                else if (messaggio.equals("-lista")) {
                    // Invia la lista degli username attualmente connessi
                    inviaListaUtenti(scriveAlClient);
             
                } 
                else {
                    // Altrimenti, invia il messaggio a tutti i client
                    broadcast(mittente + ": " + messaggio, scriveAlClient);
                    return;
                }
            } 
            else {
                controlloNumeroDeiClient();
            }
        }

        private void inviaMessaggioPrivato(String mittente, String username, String messaggioPrivato) {
            for (Map.Entry<PrintWriter, String> entry : clientMap.entrySet()) {
                if (entry.getValue().equals(username)) {
                    PrintWriter destinatario = entry.getKey();
                    destinatario.println("messaggio privato da " + mittente + ": " + messaggioPrivato);
                    return; 
                }
            }
            // Se l'utente specificato non è trovato, avvisa il mittente
            scriveAlClient.println("Utente '" + username + "' non trovato.");
        }
    }

    private void broadcast(String messaggio, PrintWriter mittenta) {
        //Invia il messaggio in broadcast
        for (PrintWriter altri : clientMap.keySet()) {
            if (altri != mittenta) {
                altri.println(messaggio);
            }
        }
    }

    private void inviaListaUtenti(PrintWriter scriveAlClient) {
        // Invia la lista degli username attualmente connessi al client specificato
        StringBuilder listaUtenti = new StringBuilder("Connected users: ");
        for (String username : clientMap.values()) {
            listaUtenti.append(username).append(", ");
        }
        // Rimuove l'ultima virgola e spazio aggiunti
        if (listaUtenti.length() > 2) {
            listaUtenti.setLength(listaUtenti.length() - 2);
        }
        scriveAlClient.println(listaUtenti.toString());
    }

    private void inviaListaUtentitBroadcast() {
        // Crea una lista di tutti gli username attualmente connessi
        StringBuilder listaUtenti = new StringBuilder("Connected users: ");
        for (String username : clientMap.values()) {
            listaUtenti.append(username).append(", ");
        }
        // Rimuove l'ultima virgola e spazio aggiunti
        if (listaUtenti.length() > 2) {
            listaUtenti.setLength(listaUtenti.length() - 2);
        }

        // Invia la lista degli username attualmente connessi in broadcast a tutti i client
        for (PrintWriter scriveAlClient : clientMap.keySet()) {
            scriveAlClient.println(listaUtenti.toString());
        }
    }

    private void controlloNumeroDeiClient() {
        // Invia un messaggio in caso ci fosse solo un client connesso
        if (clientMap.size() == 1) {
            for (PrintWriter username : clientMap.keySet()) {
                username.println("Sei l unico utente connesso, per messaggiare aspetta la connessione di un altro utente.");
            }
        }
    }


}