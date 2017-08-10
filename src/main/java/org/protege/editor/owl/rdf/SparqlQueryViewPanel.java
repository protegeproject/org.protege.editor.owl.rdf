package org.protege.editor.owl.rdf;
import  org.apache.poi.hssf.usermodel.HSSFSheet;
import  org.apache.poi.hssf.usermodel.HSSFWorkbook;
import  org.apache.poi.hssf.usermodel.HSSFRow;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableModel;
import org.protege.editor.owl.OWLEditorKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import org.protege.editor.owl.rdf.repository.BasicSparqlReasonerFactory;

/*
@Author Daniele Francesco Santamaria - University of Catania
*/

public class SparqlQueryViewPanel extends JPanel 
  {  
     static final Logger log = LoggerFactory.getLogger(SparqlQueryViewPanel.class);
     SparqlReasoner reasoner;
     JTextPane queryArea;
     JTable outArea;
     YASPPTableModel model;
     JPanel buttonArea;
     JPanel southPanel;
     JScrollPane scrollNorthArea;
     JScrollPane scrollSouthArea;     
     JSplitPane splitter;
     JButton execute;
     JButton export;
     JButton exportOpz;
     JButton importQ;
     JButton saveQ;
     OptionDialog optionD;   
     String defaultText="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+"\n"+
                        "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +"\n"+
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +"\n" +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +"\n" +
                        "SELECT ?subject ?object" +"\n" +
                        " WHERE { ?subject rdfs:subClassOf ?object }";
     OWLEditorKit editorKit;
     OptionConfig optionConfig;
     String [] bnodeVal;
    public SparqlQueryViewPanel(OWLEditorKit kit)
     {      
        bnodeVal=new String[]{"http://protege.org/owl2triplestore.owl"};
        optionConfig=new OptionConfig();
        editorKit=kit;        
        List<SparqlInferenceFactory> plugins = Collections.singletonList((SparqlInferenceFactory) new BasicSparqlReasonerFactory());
	reasoner = plugins.iterator().next().createReasoner(editorKit.getOWLModelManager().getOWLOntologyManager());
         try
           {
             reasoner.precalculate();
           } catch (SparqlReasonerException ex)
           {
             log.info(ex.toString());
           }
        setLayout(new GridLayout(1,1));
        queryArea=new JTextPane();
        queryArea.setText(defaultText);
        scrollNorthArea=new JScrollPane(queryArea);
        
        execute= new JButton("Execute Query");
        execute.addActionListener(new ExecuteActionListener());
        export= new JButton("Export Results");
        export.addActionListener(new ExportActionListener());
        exportOpz = new JButton("Options");
        exportOpz.addActionListener(new OptionActionListener());
        importQ = new JButton("Import Query");
        importQ.addActionListener(new ImportActionListener());
        saveQ=new JButton("Save Query");
        saveQ.addActionListener(new SaveActionListener());
        buttonArea = new JPanel(new FlowLayout());
        buttonArea.add(execute);
        buttonArea.add(saveQ);        
        buttonArea.add(importQ);
        buttonArea.add(export);
        buttonArea.add(exportOpz);
            
        model = new YASPPTableModel(0, 2);
        model.setColumnIdentifiers(new String[]{"?subject","?object"});
        outArea=new JTable(model);
     
        scrollSouthArea=new JScrollPane(outArea);
        outArea.setFillsViewportHeight(true);
        outArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
              {       
                   TableCellEditor cellEditor = outArea.getCellEditor();
                    if (cellEditor != null) {
                        if (!cellEditor.stopCellEditing()) {
                            cellEditor.cancelCellEditing();
                        }
                    }
                    Component gotFocus = e.getOppositeComponent();
                    if (! (gotFocus.equals(outArea) || gotFocus.equals(export) )) 
                      {
                        outArea.clearSelection(); 
                        outArea.revalidate();
                        outArea.repaint();
                      }
              }           
                 });
        
        
        southPanel=new JPanel(new BorderLayout());        
        southPanel.add(buttonArea, BorderLayout.NORTH);
        southPanel.add(scrollSouthArea, BorderLayout.CENTER);
        
        splitter=new JSplitPane(JSplitPane.VERTICAL_SPLIT,scrollNorthArea, southPanel); 
        splitter.setResizeWeight(0.7);
        add(splitter);       
      
        optionD=new OptionDialog(null);
     } 
    
    
    public void dispose() {
       if (reasoner != null) {
			reasoner.dispose();
			reasoner = null;}
    }

     public class SizeFilter extends DocumentFilter {

    private int maxCharacters;    

    public SizeFilter(int maxChars) {
        maxCharacters = maxChars;
    }

    @Override
    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
            throws BadLocationException {

        if ((fb.getDocument().getLength() + str.length()) <= maxCharacters && !str.contains("\n"))
            super.insertString(fb, offs, str, a);
        else
            Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a)
            throws BadLocationException {

        if ((fb.getDocument().getLength() + str.length()
                - length) <= maxCharacters && !str.contains("\n"))
            super.replace(fb, offs, length, str, a);
        else
            Toolkit.getDefaultToolkit().beep();
    }
}
     
    class TextOptionPanel extends JPanel
      {
        private JLabel separLabel;
        private JLabel endLabel;
        private JTextArea colSep;
        private JTextArea rowSep;
        public TextOptionPanel()
          {
                 setLayout(new FlowLayout());
                 separLabel=new JLabel("Column Separator");
                 endLabel=new JLabel("Row Separator");
                 colSep=new JTextArea(1,6);
                 colSep.setLineWrap(false);
                 colSep.setWrapStyleWord( false );                
                 ((PlainDocument) colSep.getDocument()).setDocumentFilter(new SizeFilter(6));
                                         
                 rowSep=new JTextArea(1,6);
                 rowSep.setLineWrap(false);
                 rowSep.setWrapStyleWord( false );                
                 ((PlainDocument) rowSep.getDocument()).setDocumentFilter(new SizeFilter(6));
                 
                 setLayout(new FlowLayout());
                 add(separLabel);
                 add(colSep);
                 add(endLabel);
                 add(rowSep);
          }
        public String[] getParameters(){return new String[]{colSep.getText(), rowSep.getText()};}
      }
    class ErrorQueryMessage extends JDialog
      {
          private JPanel envir;
          private JButton okbut;
          public ErrorQueryMessage(String message, String title)
            {
               setTitle(title);
               setVisible(false);         
               setModal(true);                
               setSize(400,200);
               setPreferredSize(new Dimension(400,200));
               setResizable(false);
               envir=new JPanel();
               envir.setLayout(new BorderLayout());
               JTextPane tex=new JTextPane();
               tex.setText(message);
               tex.setEditable(false);
                            
               addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent e) {
                                    dispose(); //do something
                         }});
               setDefaultCloseOperation(DISPOSE_ON_CLOSE);
               
               okbut=new JButton("OK");
               okbut.addActionListener(new ActionListener(){
                                        @Override
                                        public void actionPerformed(ActionEvent e)
                                          {                                            
                                            dispose();
                                          }
                                   });
               envir.add(new JScrollPane(tex),BorderLayout.CENTER);
               JPanel buttonPanel=new JPanel(new FlowLayout());
               buttonPanel.add(okbut);
               envir.add(buttonPanel,BorderLayout.SOUTH);
               add(envir);
               setLocationRelativeTo(null);
            }          
      }
   

  class OptionActionListener implements ActionListener
    {

        @Override
        public void actionPerformed(ActionEvent e)
          { 
           optionD.setVisible(true);
           
          }
    
    }
    
   public int isBnode(String tomatch)
     {
       for(int i=0; i< bnodeVal.length; i++)
         {
           if(tomatch.startsWith(bnodeVal[i]))
               return bnodeVal[i].length();
         
         }
       return 0;
     }
   
  
    class ExportActionListener implements ActionListener
    {

        private void toJSON(YASPPTableModel model, int[] iterator, BufferedWriter bw) throws IOException
          {            
            StringBuilder result = new StringBuilder();
            result.append("{" + "\"head\": {");
            result.append("\"vars\": [");
            for(int i=0; i<model.getColumnCount(); i++)
              {
                result.append("\"").append(model.getColumnName(i)).append("\"");
                if(i!=model.getColumnCount()-1)
                   result.append(",");
              }
            result.append("]"); //close vars
            result.append("},"); //close head
            result.append("\"results\": { ");
            result.append(" \"bindings\": ["); //open bindings
            bw.write(result.toString());
            for(int i=0; i < iterator.length; i++)
               {
                result=new StringBuilder();
                result.append("{");
                for(int j=0; j < model.getColumnCount(); j++)
                 {
                   result.append("\"").append(model.getColumnName(j)).append("\":");
                   result.append("{");
                   result.append("\"type\":");                   
                   if(!model.getValueAt( iterator[i], j).toString().startsWith("\""))
                      {
                        int c=isBnode(model.getValueAt(iterator[i], j).toString());
                        if(c>0)
                          {
                            result.append("\"bnode\",\"value\":");
                            result.append("\"").append(model.getValueAt(iterator[i], j).toString()).append("\"");
                          }
                        else
                         {
                          result.append("\"uri\",\"value\":\"");                          
                          result.append(model.getValueAt(iterator[i], j).toString()).append("\"");
                         }
                      }                 
                    else
                      {                      
                        result.append("\"literal\",\"value\":"); 
                        String value= model.getValueAt(iterator[i], j).toString();                         
                        int dataMark= value.indexOf("^");
                        String type="";
                        int initMark=dataMark+3;
                        int finMark=1;
                        if(dataMark>=0)       
                          {
                            type="datatype";
                          }
                        else
                          {
                            dataMark=value.indexOf("@"); 
                            type="xml:lang";
                            initMark=dataMark+1;
                            finMark=0;
                          }
                           String tmp=value.substring(0, dataMark);                            
                           result.append(tmp);
                        
                           result.append(",\"").append(type).append("\":\"");
                           result.append(value.substring(initMark, value.length()-finMark));
                           result.append("\"");
                          
                       }
                     result.append("}");
                     if(j!=model.getColumnCount()-1)
                     result.append(",");
                  }
                 result.append("}");
                 if(i!=iterator.length-1)
                   result.append(",");
                 bw.write(result.toString());
               }
                       
            bw.write("]}}");            
          }
        private void toExcel(YASPPTableModel model, int[] iterator, String name, FileOutputStream fileOut) throws IOException
          {
            // FileWriter fw = new FileWriter(chooser.getSelectedFile()+".xls");
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet(name);  
            for(int i=0; i < iterator.length; i++)
              {
               HSSFRow rowhead = sheet.createRow((short)i);
               for(int j=0; j < model.getColumnCount(); j++)
                {
                  rowhead.createCell(j).setCellValue(model.getValueAt(iterator[i],j).toString());                                       
                }                    
              }
            for(int i=0; i< model.getColumnCount(); i++)
             sheet.autoSizeColumn(i); 
                               
            workbook.write(fileOut);                              
          }
        private void toSimpleText(YASPPTableModel model, int [] iterator, BufferedWriter bw) throws IOException
          {
            StringBuilder result = new StringBuilder();            
            for(int i=0; i<model.getColumnCount(); i++)
              {
                result.append(model.getColumnName(i));
                if(i<model.getColumnCount()-1)
                    result.append(optionConfig.param[0]);
              }
            result.append(optionConfig.param[1]);
            bw.write(result.toString());
           
            for(int i=0; i < iterator.length; i++)
               { 
                result=new StringBuilder();               
                for(int j=0; j < model.getColumnCount(); j++)
                 {
                   result.append(model.getValueAt(iterator[i], j));
                   if(j<model.getColumnCount()-1)
                       result.append(optionConfig.param[0]);
                 }
                result.append(optionConfig.param[1]);
                bw.write(result.toString());
               }
          }

        @Override
        public void actionPerformed(ActionEvent e)
          { 
            if(model.getDataVector().isEmpty())
                 {
                   JOptionPane.showMessageDialog(null, "No query executed. Execute a query.");
                   return;
                 }
            JFileChooser chooser = new JFileChooser();     
            int retrival = chooser.showSaveDialog(null);
            if (retrival == JFileChooser.APPROVE_OPTION)
             {     
              // ProgressWorker pbarView=new ProgressWorker("Executing","Running Operation");
             //  pbarView.execute();
              
              int iterator[];
              if(outArea.getSelectedRowCount()>0)                
                {
                  iterator=new int[outArea.getSelectedRowCount()];
                  for(int i=0;i<outArea.getSelectedRowCount();i++)
                    iterator[i]= outArea.convertRowIndexToModel(outArea.getSelectedRows()[i]);
                }
              else
                {
                  iterator=new int[model.getRowCount()];
                  for(int i=0; i<model.getRowCount();i++)
                   iterator[i]=i;
                }
                
              try 
               {
                  log.info("Exporting results..."); 
                  switch (optionConfig.format)
                     {
                          case 0:
                             {  
                               FileOutputStream fileout = new FileOutputStream(chooser.getSelectedFile()+".xls");
                               toExcel(model, iterator, chooser.getSelectedFile().getName(), fileout);                             
                               fileout.close();
                               break;                       
                             }                     
                          case 1: 
                              {     
                                BufferedWriter bw = 
                                    new BufferedWriter(new FileWriter(chooser.getSelectedFile()+".srj", true));
                                toJSON(model, iterator, bw);                                 
                                bw.close();
                                break;  
                              }
                          case 2:
                            {
                                BufferedWriter bw = 
                                    new BufferedWriter(new FileWriter(chooser.getSelectedFile(), true));
                                toSimpleText(model, iterator, bw);                                 
                                bw.close();
                                break;
                            }
                          default: break;
                    }
                  log.info("Exporting successed.");
                 }
                 catch (IOException ex)
                   {
                     log.error("Error on exporting results:" + ex.toString());                     
                     ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on exporting results");           
                     messaged.setVisible(true);
                   }
              //pbarView.terminates();
              //pbarView.cancel(true);
             }           
          }    
    }
    
  class SaveActionListener implements ActionListener
  {    
    @Override
    public void actionPerformed(ActionEvent event)
      {
       JFileChooser chooser = new JFileChooser();       
      int retrival = chooser.showSaveDialog(null);
      if (retrival == JFileChooser.APPROVE_OPTION)
        {
         try 
          {
            log.info("Saving Query..."); 
            FileWriter fw = new FileWriter(chooser.getSelectedFile()+".SPARQL");
            fw.write(queryArea.getText());
            fw.close();
          } 
         catch (IOException ex)
           {
             log.error("Error on writing Query on file. "+ex.toString());
             ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on saving query");           
             messaged.setVisible(true);
           }
        }
      }
    }
  
   class ImportActionListener implements ActionListener
   {    
    @Override
    public void actionPerformed(ActionEvent event)
      {
       JFileChooser chooser = new JFileChooser();       
      int retrival = chooser.showOpenDialog(null);
      if (retrival == JFileChooser.APPROVE_OPTION)
        {
         try 
          {
            log.info("Importing query..."); 
            FileReader fr = new FileReader(chooser.getSelectedFile());
	    BufferedReader br = new BufferedReader(fr);
   	    String sCurrentLine=null;
            String fstring="";
	    while ((sCurrentLine = br.readLine()) != null)             
	       fstring+=sCurrentLine+"\n";
	    queryArea.setText(fstring);

          } 
         catch (IOException ex)
           {
             log.error("Error on reading Query file. "+ ex.toString());
             ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on reading query");           
             messaged.setVisible(true);
           }
        }
      }   
     }
   
   class ExecuteActionListener implements ActionListener
   {   
        private String[][] createData(SparqlResultSet set)
          {
            String data[][]=new String[set.getRowCount()][set.getColumnCount()];
            for(int i=0; i<set.getColumnCount(); i++)
                for(int j=0; j<set.getRowCount(); j++)
                    data[j][i]=(set.getResult(j,i).toString());
            return data;
          }
     
    @Override
    public void actionPerformed(ActionEvent event)
      {    
        try
          {    
            log.info("Executing query..."); 
            SparqlResultSet set = reasoner.executeQuery(queryArea.getText());                      
            String data[][];                  
            
            String names[]=new String[set.getColumnCount()];        
            for(int i=0; i<set.getColumnCount();i++)
                names[i]=set.getColumnName(i);
            
            data=createData(set);
            model.setDataVector(data, names);
            model.fireTableDataChanged();
            outArea.revalidate();
            outArea.repaint();
          } 
        catch (SparqlReasonerException ex)
          {      
           log.error("Error on executing query. "+ ex.toString());
           ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on executing query");           
           messaged.setVisible(true);           
          }
        log.info("Query execution successed.");
    }
  
  }

   
public class OptionDialog extends JDialog
  {
    JPanel topPanel;
    JPanel bottomPanel;
    JComboBox formatBox;
    JButton okbutton;
    TextOptionPanel textOpP;
    JPanel formatOptionContainer;
       
    public OptionDialog (JFrame parent)
     {
         super(parent, "Options");
         setSize(300,150);
         setPreferredSize(new Dimension(300,150));
         setVisible(false);         
         setModal(true);        
         setLayout(new BorderLayout());
         setResizable(false);
         addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent e) {
                                    closeDiscard(); //do something
                         }});
         
         
         formatBox=new JComboBox(new String[]{"Microsoft Excel", "SPARQL JSON", "Simple Text"});
         formatBox.setPreferredSize(new Dimension(200, formatBox.getPreferredSize().height));
         
         
         optionConfig=new OptionConfig(formatBox.getSelectedIndex());
         
         okbutton=new JButton("OK");                 
         
         topPanel=new JPanel();
         topPanel.setLayout(new FlowLayout());
         topPanel.add(new JLabel("Export Format"));
         topPanel.add(formatBox);
         
         bottomPanel=new JPanel();
         bottomPanel.setLayout(new FlowLayout());
         bottomPanel.add(okbutton);
         
        textOpP=new TextOptionPanel();
        formatOptionContainer =new JPanel();
         
         add(topPanel, BorderLayout.NORTH);
         add(bottomPanel, BorderLayout.SOUTH);
         add(formatOptionContainer);
         
         formatBox.addItemListener(new FormatActionListener(this));
         okbutton.addActionListener(new ActionListener(){
                                        @Override
                                        public void actionPerformed(ActionEvent e)
                                          {
                                            if(formatBox.getSelectedIndex()==2)
                                                optionConfig.save(formatBox.getSelectedIndex(),
                                                                  textOpP.getParameters());
                                            
                                            optionConfig.save(formatBox.getSelectedIndex());
                                            dispose();
                                          }
                                   });
         
         setLocationRelativeTo(null);
     }  
    public void closeDiscard()
      {
          formatBox.setSelectedIndex(optionConfig.format);
          dispose();
      }
    
    class FormatActionListener implements ItemListener
      {
        JDialog parent;        
        public FormatActionListener(JDialog _parent)
          {
            parent=_parent;           
          }

             @Override
             public void itemStateChanged(ItemEvent event)
               {
                 if (event.getStateChange() == ItemEvent.SELECTED)
                      {  
                        if(((JComboBox)event.getSource()).getSelectedIndex()==2)                         
                            {
                             formatOptionContainer.add(textOpP);
                            }
                       }
                 else {formatOptionContainer.removeAll();}
                 parent.revalidate();                
                 parent.repaint();
               }
      }
  }

  }

 class YASPPTableModel extends DefaultTableModel 
   {

    public YASPPTableModel(int x, int y)
      {
         super(x, y);         
          
      }
           
    public boolean isCellEditable() {
          return false;
            }
     }

class OptionConfig
  {
     int format;
     String[] param;
     public OptionConfig(){
         format=-1; param=new String[]{};
     }
     public OptionConfig(int value){format=value; param=new String[]{};}
     public void save(int _format){format=_format;}
     public void save(int _format, String[] _param)
       {
         save(_format);
         param=_param;
       }
  }

 
