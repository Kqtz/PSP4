package es.studium.psp4;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.io.PrintWriter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class clienteFTPBasico extends JFrame 
{
	private static final long serialVersionUID = 1L;
	
	// --- CAMPOS SUPERIORES (2 filas x 3 columnas) ---
	static JTextField txtServidor               = new JTextField();
	static JTextField txtUsuario                = new JTextField();
	static JTextField txtDirectorioRaiz         = new JTextField();
	static JTextField txtArbolDirectorios       = new JTextField();
	static JTextField txtRutaLocal              = new JTextField();
	
	// --- CAMPOS DE ESTADO / MENSAJES (se reutiliza uno de ellos para mostrar info) ---
	private static JTextField txtActualizarArbol = new JTextField();
	
	// --- LISTA DE CONTENIDO REMOTO ---
	static JList<String> listaDirec = new JList<>();
	private JScrollPane barraDesplazamiento;
	
	// --- BOTONES ---
	JButton botonCrearDir         = new JButton("Crear carpeta");
	JButton botonBorrarDir        = new JButton("Eliminar carpeta");
	JButton botonRenombrarDir     = new JButton("Renombrar carpeta");
	JButton botonCargarFichero    = new JButton("Subir fichero");
	JButton botonDescargarFichero = new JButton("Descargar fichero");
	JButton botonRenombrarFichero = new JButton("Renombrar fichero");
	JButton botonBorrarFichero    = new JButton("Eliminar fichero");
	JButton botonVolver           = new JButton("Atrás");
	JButton botonEstablecerRuta   = new JButton("Establecer ruta local");
	JButton botonSalir            = new JButton("Salir");
	
	// --- CLIENTE FTP ---
	static FTPClient cliente = new FTPClient();
	String servidor = "127.0.0.1";
	String user     = "Alvaro";
	String pasw     = "Studium";
	boolean login;
	
	// --- DIRECTORIO DE TRABAJO ACTUAL ---
	static String direcInicial = "/";
	static String direcSelec   = direcInicial;
	static String ficheroSelec = "";
	
	// --- RUTA LOCAL DE DESCARGA (vacía => se usa JFileChooser) ---
	String rutaLocalDescarga = "";

	public static void main(String[] args) throws IOException 
	{
		new clienteFTPBasico();
	}

	public clienteFTPBasico() throws IOException
	{
		super("CLIENTE BÁSICO FTP");
		
		// Para ver en consola los comandos que se originan
		cliente.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
		
		// Conexión al servidor
		cliente.connect(servidor);
		cliente.enterLocalPassiveMode();
		login = cliente.login(user, pasw);
		
		// Directorio de trabajo inicial
		cliente.changeWorkingDirectory(direcInicial);
		FTPFile[] files = cliente.listFiles();
		
		// Llenamos la lista inicial
		llenarLista(files, direcInicial);

		// --------------------------------------------------------------------
		// DISEÑO DE LA INTERFAZ (SIMILAR A LA SEGUNDA IMAGEN)
		// --------------------------------------------------------------------
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 600);                           // Ventana ancha
		getContentPane().setBackground(Color.LIGHT_GRAY); 
		setLayout(new BorderLayout(10, 10));         // Margen entre paneles
		setLocationRelativeTo(null);                 // Centrar en pantalla
		
		// 1) PANEL SUPERIOR (2x3) con fondo blanco y margen
		JPanel panelSuperior = new JPanel(new GridLayout(2, 3, 5, 5));
		panelSuperior.setBackground(Color.WHITE);
		panelSuperior.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		// Configuramos los 5 textfields
		txtServidor.setEditable(false);
		txtServidor.setText("Servidor FTP: " + servidor);
		
		txtUsuario.setEditable(false);
		txtUsuario.setText("Usuario: " + user);
		
		txtDirectorioRaiz.setEditable(false);
		txtDirectorioRaiz.setText("DIRECTORIO RAÍZ: " + direcInicial);
		
		txtArbolDirectorios.setEditable(false);
		txtArbolDirectorios.setText("<< ÁRBOL DE DIRECTORIOS CONSTRUIDO >>");
		
		txtRutaLocal.setEditable(false);
		txtRutaLocal.setText("Ruta local no establecida");
		
		// Añadimos en orden a la rejilla 2x3
		panelSuperior.add(txtServidor);
		panelSuperior.add(txtUsuario);
		panelSuperior.add(txtDirectorioRaiz);
		panelSuperior.add(txtArbolDirectorios);
		panelSuperior.add(txtRutaLocal);
		
		// Sexta celda "vacía" (o si quieres, un label)
		panelSuperior.add(new JLabel(""));
		
		// Añadimos el panel superior al BorderLayout.NORTH
		add(panelSuperior, BorderLayout.NORTH);
		
		// 2) PANEL CENTRAL (también blanco y con margen)
		JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
		panelCentral.setBackground(Color.WHITE);
		panelCentral.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		// Lista de ficheros/carpetas a la IZQUIERDA
		barraDesplazamiento = new JScrollPane(listaDirec);
		listaDirec.setForeground(Color.BLUE);
		listaDirec.setFont(new Font("Courier", Font.PLAIN, 12));
		listaDirec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		panelCentral.add(barraDesplazamiento, BorderLayout.CENTER);
		
		// Panel de BOTONES a la DERECHA (vertical)
		JPanel panelBotones = new JPanel(new GridLayout(10, 1, 5, 5));
		panelBotones.setBackground(Color.WHITE);
		
		// Tamaño preferido para que todos sean similares
		Dimension btnSize = new Dimension(180, 30);
		
		botonCrearDir.setPreferredSize(btnSize);
		botonBorrarDir.setPreferredSize(btnSize);
		botonRenombrarDir.setPreferredSize(btnSize);
		botonCargarFichero.setPreferredSize(btnSize);
		botonDescargarFichero.setPreferredSize(btnSize);
		botonRenombrarFichero.setPreferredSize(btnSize);
		botonBorrarFichero.setPreferredSize(btnSize);
		botonVolver.setPreferredSize(btnSize);
		botonEstablecerRuta.setPreferredSize(btnSize);
		botonSalir.setPreferredSize(btnSize);
		
		panelBotones.add(botonCrearDir);
		panelBotones.add(botonBorrarDir);
		panelBotones.add(botonRenombrarDir);
		panelBotones.add(botonCargarFichero);
		panelBotones.add(botonDescargarFichero);
		panelBotones.add(botonRenombrarFichero);
		panelBotones.add(botonBorrarFichero);
		panelBotones.add(botonVolver);
		panelBotones.add(botonEstablecerRuta);
		panelBotones.add(botonSalir);
		
		panelCentral.add(panelBotones, BorderLayout.EAST);
		
		// Añadimos el panel central al BorderLayout.CENTER
		add(panelCentral, BorderLayout.CENTER);
		
		// 3) TXT DE ESTADO OPCIONAL ABAJO (si lo deseas)
		// Aquí usamos txtActualizarArbol para mostrar mensajes
		txtActualizarArbol.setEditable(false);
		add(txtActualizarArbol, BorderLayout.SOUTH);
		
		setVisible(true);

		// --------------------------------------------------------------------
		// LISTENERS (MISMA LÓGICA QUE ANTES)
		// --------------------------------------------------------------------
		
		// Selección en la lista (un solo clic)
		listaDirec.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent lse)
			{
				if (!lse.getValueIsAdjusting()) 
				{
					String valorSeleccionado = listaDirec.getSelectedValue();
					if (valorSeleccionado != null)
					{
						if (valorSeleccionado.startsWith("(DIR) "))
						{
							ficheroSelec = valorSeleccionado.substring(6);
							txtArbolDirectorios.setText("DIRECTORIO SELECCIONADO: " + ficheroSelec);
						}
						else
						{
							ficheroSelec = valorSeleccionado;
							txtArbolDirectorios.setText("FICHERO SELECCIONADO: " + ficheroSelec);
						}
						txtActualizarArbol.setText("DIRECTORIO ACTUAL: " + direcSelec);
					}
				}
			}
		});
		
		// Doble clic en la lista para entrar en directorio
		listaDirec.addMouseListener(new MouseAdapter() 
		{
			public void mouseClicked(MouseEvent e) 
			{
				if (e.getClickCount() == 2 && !e.isConsumed()) 
				{
					e.consume();
					int index = listaDirec.locationToIndex(e.getPoint());
					if (index >= 0) 
					{
						String valorSeleccionado = listaDirec.getModel().getElementAt(index);
						if (valorSeleccionado.startsWith("(DIR) "))
						{
							String nombreDir = valorSeleccionado.substring(6);
							try 
							{
								if (direcSelec.equals("/"))
								{
									direcSelec = "/" + nombreDir;
								}
								else
								{
									direcSelec = direcSelec + "/" + nombreDir;
								}
								cliente.changeWorkingDirectory(direcSelec);
								FTPFile[] ff2 = cliente.listFiles();
								llenarLista(ff2, direcSelec);
								txtActualizarArbol.setText("DIRECTORIO ACTUAL: " + direcSelec);
							} 
							catch (IOException ex) 
							{
								ex.printStackTrace();
							}
						}
					}
				}
			}
		});
		
		// Botón SALIR
		botonSalir.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try 
				{
					cliente.disconnect();
				}
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
				System.exit(0);
			}
		});
		
		// Botón CREAR DIRECTORIO
		botonCrearDir.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String nombreCarpeta = JOptionPane.showInputDialog(null, 
						"Introduce el nombre de la carpeta", "Nueva carpeta");
				if (nombreCarpeta != null && !nombreCarpeta.trim().isEmpty()) 
				{
					String directorio = direcSelec;
					if (!direcSelec.equals("/"))
						directorio += "/";
					directorio += nombreCarpeta.trim(); 
					try 
					{
						if (cliente.makeDirectory(directorio))
						{
							String m = nombreCarpeta.trim() + " => Se ha creado correctamente ...";
							JOptionPane.showMessageDialog(null, m);
							txtArbolDirectorios.setText(m);
							cliente.changeWorkingDirectory(direcSelec);
							FTPFile[] ff2 = cliente.listFiles();
							llenarLista(ff2, direcSelec);
						}
						else
						{
							JOptionPane.showMessageDialog(null, 
									nombreCarpeta.trim() + " => No se ha podido crear ...");
						}
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		});
		
		// Botón ELIMINAR DIRECTORIO
		botonBorrarDir.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!esDirectorioSeleccionado())
				{
					JOptionPane.showMessageDialog(null, 
							"ERROR: Se ha seleccionado un fichero, no un directorio",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				int confirm = JOptionPane.showConfirmDialog(null,
						"¿Seguro que deseas eliminar la carpeta " + ficheroSelec + "?", 
						"Eliminar carpeta", JOptionPane.YES_NO_OPTION);
				
				if (confirm == JOptionPane.YES_OPTION)
				{
					try 
					{
						String directorio = direcSelec;
						if (!direcSelec.equals("/"))
							directorio += "/";
						directorio += ficheroSelec; 
						
						if (cliente.removeDirectory(directorio))
						{
							String m = ficheroSelec + " => Se ha eliminado correctamente ...";
							JOptionPane.showMessageDialog(null, m);
							txtArbolDirectorios.setText(m);
							cliente.changeWorkingDirectory(direcSelec);
							FTPFile[] ff2 = cliente.listFiles();
							llenarLista(ff2, direcSelec);
						}
						else
						{
							JOptionPane.showMessageDialog(null, 
									ficheroSelec + " => No se ha podido eliminar ...");
						}
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		});
		
		// Botón SUBIR FICHERO
		botonCargarFichero.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser f = new JFileChooser();
				f.setFileSelectionMode(JFileChooser.FILES_ONLY);
				f.setDialogTitle("Selecciona el fichero a subir al servidor FTP");
				int returnVal = f.showDialog(f, "Subir");
				if (returnVal == JFileChooser.APPROVE_OPTION) 
				{
					File file = f.getSelectedFile();
					String rutaCompleta = file.getAbsolutePath();
					String nombreArchivo = file.getName();
					try 
					{
						SubirFichero(rutaCompleta, nombreArchivo);
					}
					catch (IOException e1) 
					{
						e1.printStackTrace(); 
					}
				}
			}
		});
		
		// Botón DESCARGAR FICHERO
		botonDescargarFichero.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!esFicheroSeleccionado())
				{
					JOptionPane.showMessageDialog(null, 
							"ERROR: Se ha seleccionado un directorio, no un fichero",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String directorio = direcSelec;
				if (!direcSelec.equals("/"))
					directorio += "/";
				
				if (!ficheroSelec.isEmpty()) 
				{
					DescargarFichero(directorio + ficheroSelec, ficheroSelec);
				}
			}
		});
		
		// Botón BORRAR FICHERO
		botonBorrarFichero.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!esFicheroSeleccionado())
				{
					JOptionPane.showMessageDialog(null, 
							"ERROR: Se ha seleccionado un directorio, no un fichero",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String directorio = direcSelec;
				if (!direcSelec.equals("/"))
					directorio += "/";
				if (!ficheroSelec.equals("")) 
				{
					BorrarFichero(directorio + ficheroSelec, ficheroSelec);
				}
			}
		});
		
		// Botón RENOMBRAR FICHERO
		botonRenombrarFichero.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!esFicheroSeleccionado())
				{
					JOptionPane.showMessageDialog(null, 
							"ERROR: Se ha seleccionado un directorio, no un fichero",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String nuevoNombre = JOptionPane.showInputDialog(null,
						"Introduce el nuevo nombre para el fichero", ficheroSelec);
				
				if (nuevoNombre != null && !nuevoNombre.trim().isEmpty())
				{
					try 
					{
						String directorio = direcSelec;
						if (!direcSelec.equals("/"))
							directorio += "/";
						
						String rutaAntigua = directorio + ficheroSelec;
						String rutaNueva   = directorio + nuevoNombre;
						
						boolean exito = cliente.rename(rutaAntigua, rutaNueva);
						if (exito)
						{
							JOptionPane.showMessageDialog(null,
									"El fichero se ha renombrado correctamente a: " + nuevoNombre);
							FTPFile[] ff2 = cliente.listFiles();
							llenarLista(ff2, direcSelec);
						}
						else
						{
							JOptionPane.showMessageDialog(null,
									"No se pudo renombrar el fichero");
						}
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		});
		
		// Botón RENOMBRAR DIRECTORIO
		botonRenombrarDir.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!esDirectorioSeleccionado())
				{
					JOptionPane.showMessageDialog(null, 
							"ERROR: Se ha seleccionado un fichero, no un directorio",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String nuevoNombre = JOptionPane.showInputDialog(null,
						"Introduce el nuevo nombre para la carpeta", ficheroSelec);
				
				if (nuevoNombre != null && !nuevoNombre.trim().isEmpty())
				{
					try 
					{
						String rutaPadre = direcSelec;
						if (!rutaPadre.equals("/"))
							rutaPadre += "/";
						
						String directorioViejo = rutaPadre + ficheroSelec;
						String directorioNuevo = rutaPadre + nuevoNombre;
						
						boolean exito = cliente.rename(directorioViejo, directorioNuevo);
						if (exito)
						{
							JOptionPane.showMessageDialog(null,
									"El directorio se ha renombrado correctamente a: " + nuevoNombre);
							FTPFile[] ff2 = cliente.listFiles();
							llenarLista(ff2, direcSelec);
						}
						else
						{
							JOptionPane.showMessageDialog(null,
									"No se pudo renombrar el directorio");
						}
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		});
		
		// Botón ATRÁS (subir al directorio padre)
		botonVolver.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					if (direcSelec.equals("/"))
					{
						JOptionPane.showMessageDialog(null,
								"Ya estás en el directorio raíz", 
								"Info", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					
					cliente.changeToParentDirectory();
					direcSelec = cliente.printWorkingDirectory();
					if (direcSelec == null || direcSelec.trim().equals(""))
					{
						direcSelec = "/";
					}
					
					FTPFile[] ff2 = cliente.listFiles();
					llenarLista(ff2, direcSelec);
					txtActualizarArbol.setText("DIRECTORIO ACTUAL: " + direcSelec);
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
		});
		
		// Botón ESTABLECER RUTA LOCAL
		botonEstablecerRuta.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Avisamos que vamos a cambiar la ruta local
				String ruta = JOptionPane.showInputDialog(null,
						"Introduce la ruta local para descargas",
						rutaLocalDescarga.isEmpty() ? "C:\\Descargas" : rutaLocalDescarga);
				
				if (ruta == null || ruta.trim().isEmpty())
				{
					JOptionPane.showMessageDialog(null, 
							"Ruta local vacía. Se usará el diálogo para descargar.", 
							"Info", JOptionPane.INFORMATION_MESSAGE);
					rutaLocalDescarga = "";
					txtRutaLocal.setText("Ruta local no establecida");
				}
				else
				{
					File f = new File(ruta);
					if (!f.exists() || !f.isDirectory())
					{
						JOptionPane.showMessageDialog(null, 
								"La ruta especificada no existe o no es un directorio válido.",
								"Error", JOptionPane.ERROR_MESSAGE);
					}
					else
					{
						rutaLocalDescarga = ruta;
						txtRutaLocal.setText(rutaLocalDescarga);
						JOptionPane.showMessageDialog(null, 
								"Ruta local establecida: " + rutaLocalDescarga,
								"OK", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}
		});
	}
	
	/**
	 * Rellena la lista con los ficheros y directorios del array 'files'
	 */
	private static void llenarLista(FTPFile[] files, String dirActual) 
	{
		if (files == null) return;
		
		DefaultListModel<String> modeloLista = new DefaultListModel<>();
		listaDirec.removeAll();
		
		try 
		{
			cliente.changeWorkingDirectory(dirActual);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		direcSelec = dirActual;
		
		for (FTPFile f : files)
		{
			String nombre = f.getName();
			if (!nombre.equals(".") && !nombre.equals(".."))
			{
				if (f.isDirectory())
				{
					modeloLista.addElement("(DIR) " + nombre);
				}
				else
				{
					modeloLista.addElement(nombre);
				}
			}
		}
		
		listaDirec.setModel(modeloLista);
	}
	
	/**
	 * Sube un fichero al directorio actual en el servidor FTP
	 */
	private boolean SubirFichero(String rutaLocal, String nombreFichero) throws IOException 
	{
		cliente.setFileType(FTP.BINARY_FILE_TYPE);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(rutaLocal));
		boolean ok = false;
		
		cliente.changeWorkingDirectory(direcSelec);
		if (cliente.storeFile(nombreFichero, in)) 
		{
			String s = nombreFichero + " => Subido correctamente...";
			txtArbolDirectorios.setText(s);
			txtActualizarArbol.setText("DIRECTORIO ACTUAL: " + direcSelec);
			JOptionPane.showMessageDialog(null, s);
			FTPFile[] ff2 = cliente.listFiles();
			llenarLista(ff2, direcSelec);
			ok = true;
		}
		else
		{
			txtArbolDirectorios.setText("No se ha podido subir... " + nombreFichero);
		}
		in.close();
		return ok;
	}
	
	/**
	 * Descarga un fichero desde el servidor FTP a la ruta local establecida 
	 * o, si está vacía, a la elegida por JFileChooser
	 */
	private void DescargarFichero(String nombreCompletoRemoto, String nombreFichero) 
	{
		try
		{
			cliente.setFileType(FTP.BINARY_FILE_TYPE);
			
			String archivoyCarpetaDestino = "";
			
			if (rutaLocalDescarga.isEmpty())
			{
				JFileChooser f = new JFileChooser();
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				f.setDialogTitle("Selecciona el Directorio donde Descargar el Fichero");
				int returnVal = f.showDialog(null, "Descargar");
				if (returnVal == JFileChooser.APPROVE_OPTION) 
				{
					File file = f.getSelectedFile();
					String carpetaDestino = file.getAbsolutePath();
					archivoyCarpetaDestino = carpetaDestino + File.separator + nombreFichero;
				}
				else
				{
					return; // Usuario canceló
				}
			}
			else
			{
				archivoyCarpetaDestino = rutaLocalDescarga + File.separator + nombreFichero;
			}
			
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(archivoyCarpetaDestino));
			if (cliente.retrieveFile(nombreCompletoRemoto, out))
			{
				JOptionPane.showMessageDialog(null,
						nombreFichero + " => Se ha descargado correctamente ...");
			}
			else
			{
				JOptionPane.showMessageDialog(null,
						nombreFichero + " => No se ha podido descargar ...");
			}
			out.close();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	/**
	 * Borra un fichero en el servidor FTP
	 */
	private void BorrarFichero(String nombreCompleto, String nombreFichero) 
	{
		int seleccion = JOptionPane.showConfirmDialog(
			    null, 
			    "¿Desea eliminar el fichero seleccionado?", 
			    "Borrar fichero", 
			    JOptionPane.YES_NO_OPTION
			);
		if (seleccion == JOptionPane.OK_OPTION) 
		{
			try 
			{
				if (cliente.deleteFile(nombreCompleto)) 
				{
					String m = nombreFichero + " => Eliminado correctamente... ";
					JOptionPane.showMessageDialog(null, m);
					txtArbolDirectorios.setText(m);
					cliente.changeWorkingDirectory(direcSelec);
					FTPFile[] ff2 = cliente.listFiles();
					llenarLista(ff2, direcSelec);
				}
				else
				{
					JOptionPane.showMessageDialog(null, 
							nombreFichero + " => No se ha podido eliminar ...");
				}
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Indica si el elemento seleccionado en la lista es un directorio
	 */
	private boolean esDirectorioSeleccionado()
	{
		if (ficheroSelec == null || ficheroSelec.trim().isEmpty()) return false;
		String seleccionado = listaDirec.getSelectedValue();
		return (seleccionado != null && seleccionado.startsWith("(DIR) "));
	}
	
	/**
	 * Indica si el elemento seleccionado en la lista es un fichero
	 */
	private boolean esFicheroSeleccionado()
	{
		if (ficheroSelec == null || ficheroSelec.trim().isEmpty()) return false;
		String seleccionado = listaDirec.getSelectedValue();
		return (seleccionado != null && !seleccionado.startsWith("(DIR) "));
	}
}
