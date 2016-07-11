import java.io.File;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

import static org.semanticweb.owlapi.search.EntitySearcher.getAnnotationObjects;

public class OntobeeQuery {
	public static void main(String[] args){
		File file = new File("obi.owl");
	}
	
	public static OWLOntology parseOntology(File file){
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		IRI documentIRI = IRI.create(file);
		OWLOntology ontology = null;
		
		try {
			ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
			
			System.out.println("Ontology Loaded...");
	        System.out.println("Document IRI: " + documentIRI);
	        System.out.println("Ontology : " + ontology.getOntologyID());
	        System.out.println("Format      : " + manager.getOntologyFormat(ontology));

	        OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
	        System.out.println("Class       : " + clazz);
	        // Print the hierarchy below thing
	        OWLReasoner reasoner = new StructuralReasoner(ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
	        printHierarchy(clazz, reasoner, ontology);
	        
		} catch (OWLException e) {
			e.printStackTrace();
		}
		return ontology;
	}
	
	 private static void printHierarchy(@Nonnull OWLClass clazz, OWLReasoner reasoner, OWLOntology ontology) throws OWLException {
	        printHierarchy(reasoner, clazz, 0, ontology);
	        /* Now print out any unsatisfiable classes */
	        for (OWLClass cl : ontology.getClassesInSignature()) {
	            assert cl != null;
	            if (!reasoner.isSatisfiable(cl)) {
	            	System.out.println("XXX: " + labelFor(cl, ontology));
	            }
	        }
	        reasoner.dispose();
	    }
	 
	 private static void printHierarchy(@Nonnull OWLReasoner reasoner, @Nonnull OWLClass clazz, int level, OWLOntology ontology) throws OWLException {
	        /*
	         * Only print satisfiable classes -- otherwise we end up with bottom
	         * everywhere
	         */
	        if (reasoner.isSatisfiable(clazz)) {
	            for (int i = 0; i < level * 4; i++) {
	            	//System.out.print(" ");
	            }
	            String s = labelFor(clazz, ontology);
	            if(s.contains("IC50")){
	            	System.out.println(labelFor(clazz, ontology));
	            	for (OWLClass heir: reasoner.getSuperClasses(clazz, true).getFlattened()){
	            		System.out.println(labelFor(heir, ontology));
	            	}
	            }
	            
	            /* Find the children and recurse */
	            for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
	                if (!child.equals(clazz)) {
	                    printHierarchy(reasoner, child, level + 1, ontology);
	                }
	            }
	        }
	    }
	 

	    private static String labelFor(@Nonnull OWLClass clazz, OWLOntology ontology) {
	        /*
	         * Use a visitor to extract label annotations
	         */
	        LabelExtractor le = new LabelExtractor();
	        for (OWLAnnotation anno : getAnnotationObjects(clazz, ontology)) {
	            anno.accept(le);
	        }
	        /* Print out the label if there is one. If not, just use the class URI */
	        if (le.getResult() != null) {
	            return le.getResult();
	        } else {
	            return clazz.getIRI().toString();
	        }
	    }
}
