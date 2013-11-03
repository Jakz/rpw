package net.mightypork.rpack.gui.windows;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import net.mightypork.rpack.App;
import net.mightypork.rpack.Config;
import net.mightypork.rpack.Paths;
import net.mightypork.rpack.gui.Icons;
import net.mightypork.rpack.gui.helpers.CharInputListener;
import net.mightypork.rpack.gui.helpers.FilenameKeyAdapter;
import net.mightypork.rpack.gui.widgets.FileNameList;
import net.mightypork.rpack.hierarchy.EAsset;
import net.mightypork.rpack.library.Sources;
import net.mightypork.rpack.tasks.Tasks;
import net.mightypork.rpack.utils.FileUtils;
import net.mightypork.rpack.utils.OsUtils;
import net.mightypork.rpack.utils.Utils;
import net.mightypork.rpack.utils.ZipUtils;
import net.mightypork.rpack.utils.filters.FileSuffixFilter;
import net.mightypork.rpack.utils.filters.StringFilter;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.JXTitledSeparator;


public class DialogImportPack extends RpwDialog {

	private List<String> options;

	private JXTextField field;

	private JButton buttonOK;


	private FileNameList list;

	private JXLabel importUrl;

	private JButton buttonPickFile;

	private JFileChooser fc;

	private File selectedFile;

	private JButton buttonCancel;


	public DialogImportPack() {

		super(App.getFrame(), "Import ResourcePack");

		Box hb;
		Box vb = Box.createVerticalBox();
		vb.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		vb.add(new JXTitledSeparator("File to import"));

		//@formatter:off
		hb = Box.createHorizontalBox();
			importUrl = new JXLabel(" ");
			importUrl.setToolTipText("Imported ZIP file");
			importUrl.setForeground(new Color(0x111111));
			importUrl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
			importUrl.setHorizontalAlignment(SwingConstants.RIGHT);
			importUrl.setMaximumSize(new Dimension(300, 25));
	
			buttonPickFile = new JButton(Icons.MENU_OPEN);
			buttonPickFile.requestFocusInWindow();
	
			hb.add(buttonPickFile);
			hb.add(Box.createHorizontalStrut(5));
			hb.add(importUrl);
			hb.add(Box.createHorizontalGlue());
	
			hb.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		vb.add(hb);
		//@formatter:on

		vb.add(Box.createVerticalStrut(8));

		options = Sources.getResourcepackNames();

		vb.add(list = new FileNameList(options, true));
		list.list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {

				String s = list.getSelectedValue();
				if (s != null) {
					field.setText(s);
					buttonOK.setEnabled(false);
				}

			}
		});

		vb.add(Box.createVerticalStrut(10));

		//@formatter:off
		hb = Box.createHorizontalBox();
			JXLabel label = new JXLabel("Name:");
			hb.add(label);
			hb.add(Box.createHorizontalStrut(5));
	
			field = new JXTextField();
			Border bdr = BorderFactory.createCompoundBorder(field.getBorder(), BorderFactory.createEmptyBorder(3,3,3,3));
			field.setBorder(bdr);
			
			CharInputListener listener = new CharInputListener() {
				
				@Override
				public void onCharTyped(char c) {
				
					String s = (field.getText() + c).trim();
					
					boolean ok = true;
					ok &= (s.length() > 0);
					ok &= !options.contains(s);
					ok &= selectedFile != null;
					
					buttonOK.setEnabled(ok);	
				}
			};
			
			field.addKeyListener(new FilenameKeyAdapter(listener));
			
			
			hb.add(field);
		vb.add(hb);

		
		vb.add(Box.createVerticalStrut(8));

		
		hb = Box.createHorizontalBox();
			hb.add(Box.createHorizontalGlue());
	
			buttonOK = new JButton("Import", Icons.MENU_YES);
			buttonOK.setEnabled(false);
			hb.add(buttonOK);
	
			hb.add(Box.createHorizontalStrut(5));
	
			buttonCancel = new JButton("Cancel", Icons.MENU_CANCEL);
			hb.add(buttonCancel);
		vb.add(hb);
		//@formatter:on

		getContentPane().add(vb);

		prepareForDisplay();

		initFileChooser();
	}


	private void initFileChooser() {

		fc = new JFileChooser();
		fc.setAcceptAllFileFilterUsed(false);
		fc.setDialogTitle("Import ResourcePack archive");
		fc.setFileFilter(new FileFilter() {

			FileSuffixFilter fsf = new FileSuffixFilter("zip", "jar");


			@Override
			public String getDescription() {

				return "ZIP archives";
			}


			@Override
			public boolean accept(File f) {

				if (f.isDirectory()) return true;
				return fsf.accept(f);
			}
		});

		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setFileHidingEnabled(!Config.SHOW_HIDDEN_FILES);
	}


	@Override
	protected void addActions() {

		buttonPickFile.addActionListener(pickFileListener);
		buttonOK.addActionListener(importListener);
		buttonCancel.addActionListener(closeListener);
	}


	@Override
	public void onClose() {

		Tasks.taskReloadSources(null);
	}

	private ActionListener importListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {

			String name = field.getText().trim();
			if (name.length() == 0) {
				Alerts.error(DialogImportPack.this, "Invalid name", "Missing source name!");
			}

			if (options.contains(name)) {
				Alerts.error(DialogImportPack.this, "Invalid name", "Source named \"" + name + "\" already exists!");
			} else {

				File out = OsUtils.getAppDir(Paths.DIR_RESOURCEPACKS + "/" + name, true);

				StringFilter filter = new StringFilter() {

					@Override
					public boolean accept(String path) {

						boolean ok = false;

						String[] parts = FileUtils.removeExtension(path);
						String ext = parts[1];
						EAsset type = EAsset.forExtension(ext);

						ok |= path.startsWith("assets");
						ok &= ((type != null) || ext.equals("mcmeta"));

						return ok;
					}
				};

				try {
					ZipUtils.extractZip(selectedFile, out, filter);
					closeDialog();
					Alerts.info(App.getFrame(), "Resource Pack \"" + name + "\" was imported.");

				} catch (Exception exc) {
					Alerts.error(DialogImportPack.this, "Error while extracting ResourcePack zip.");
					FileUtils.delete(out, true); // cleanup
				}

			}

		}
	};

	private ActionListener pickFileListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {

			int btn = fc.showDialog(DialogImportPack.this, "Import");
			if (btn == JFileChooser.APPROVE_OPTION) {
				File f = fc.getSelectedFile();

				if (f == null) return;

				if (f.exists()) {
					selectedFile = f;

					String path = f.getPath();
					int length = 28;
					path = Utils.cropStringAtStart(path, length);

					importUrl.setText(path);

					try {
						String[] parts = FileUtils.removeExtension(f);
						field.setText(parts[0]);

						buttonOK.setEnabled(!options.contains(parts[0]));
					} catch (Throwable t) {}
				}
			}
		}
	};
}