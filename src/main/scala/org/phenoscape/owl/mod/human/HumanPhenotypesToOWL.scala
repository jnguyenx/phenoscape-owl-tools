package org.phenoscape.owl.mod.human

import java.io.File
import scala.collection.JavaConversions._
import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.collection.Set
import scala.collection.mutable
import scala.io.Source
import org.phenoscape.scowl.OWL._
import org.apache.commons.lang3.StringUtils
import org.phenoscape.owl.util.OBOUtil
import org.phenoscape.owl.OWLTask
import org.phenoscape.owl.Vocab
import org.phenoscape.owl.Vocab._
import org.semanticweb.owlapi.model.AddImport
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAxiom
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import org.semanticweb.owlapi.apibinding.OWLManager
import org.phenoscape.owl.util.OntologyUtil

object HumanPhenotypesToOWL extends OWLTask {

  val rdfsLabel = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI())
  val human = factory.getOWLNamedIndividual(Vocab.HUMAN)
  val manager = OWLManager.createOWLOntologyManager()

  def main(args: Array[String]): Unit = {
    val file = Source.fromFile(args(0), "utf-8")
    val ontology = convert(file)
    file.close()
    manager.saveOntology(ontology, IRI.create(new File(args(1))))
  }

  def convert(phenotypeData: Source): OWLOntology = {
    val ontology = manager.createOntology(IRI.create("http://purl.obolibrary.org/obo/phenoscape/human_phenotypes.owl"))
    manager.addAxioms(ontology, phenotypeData.getLines.drop(1).map(translate(_)).flatten.toSet[OWLAxiom])
    return ontology
  }

  def translate(phenotypeLine: String): Set[OWLAxiom] = {
    val items = phenotypeLine.split("\t")
    val axioms = mutable.Set.empty[OWLAxiom]
    val phenotype = OntologyUtil.nextIndividual()
    axioms.add(phenotype Type AnnotatedPhenotype)
    axioms.add(factory.getOWLDeclarationAxiom(phenotype))
    val phenotypeID = StringUtils.stripToNull(items(3))
    val phenotypeClass = Class(OBOUtil.iriForTermID(phenotypeID))
    axioms.add(phenotype Type phenotypeClass)
    val geneIRI = IRI.create("http://www.ncbi.nlm.nih.gov/gene/" + StringUtils.stripToNull(items(0)))
    val geneSymbol = StringUtils.stripToNull(items(1))
    axioms.add(geneIRI Annotation (rdfsLabel, factory.getOWLLiteral(geneSymbol)))
    val gene = Individual(geneIRI)
    axioms.add(gene Type Gene)
    axioms.add(factory.getOWLDeclarationAxiom(gene))
    axioms.add(phenotype Fact (associated_with_gene, gene))
    axioms.add(phenotype Fact (associated_with_taxon, human))
    return axioms
  }

}