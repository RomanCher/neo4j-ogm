package org.neo4j.ogm.strategy.simple;

import java.util.ArrayList;
import java.util.List;

import org.graphaware.graphmodel.Property;
import org.graphaware.graphmodel.neo4j.EdgeModel;
import org.graphaware.graphmodel.neo4j.GraphModel;
import org.graphaware.graphmodel.neo4j.NodeModel;
import org.neo4j.ogm.mapper.GraphModelToObjectMapper;
import org.neo4j.ogm.metadata.MappingMetadata;
import org.neo4j.ogm.metadata.ObjectCreator;
import org.neo4j.ogm.metadata.PersistentField;
import org.neo4j.ogm.strategy.EntityAccessStrategyFactory;

/**
 * A copy of {@link SimpleMappingStrategy} to show the separation of "the what" from "the how" - this is essentially the thing
 * that contains the algorithm for joining them up.
 */
public class CopiedSimpleMappingStrategy implements GraphModelToObjectMapper<GraphModel> {

    private final MappingContext mappingContext;
    private final ObjectCreator objectCreator;
    private final EntityAccessStrategyFactory entityAccessStrategyFactory;
    private final MappingMetadata mappingMetadata;

    /**
     * @param type The type of the root object
     * @param objectCreationStrategy The {@link ObjectCreator} to use for instantiating types
     * @param entityAccessStrategyFactory To determine how the property values should be mapped to the fields
     * @param mappingMetadata Contains information about how fields should be mapped to properties and vice versa
     */
    // there may be a case for encapsulating these params in MappingConfiguration
    public CopiedSimpleMappingStrategy(Class<?> type, ObjectCreator objectCreationStrategy,
            EntityAccessStrategyFactory entityAccessStrategyFactory, MappingMetadata mappingMetadata) {
        this.mappingContext = new MappingContext(type);
        this.objectCreator = objectCreationStrategy;
        this.entityAccessStrategyFactory = entityAccessStrategyFactory;
        this.mappingMetadata = mappingMetadata;
    }

    @Override
    public Object mapToObject(GraphModel graphModel) {
        try {
            mapNodes(graphModel);
            mapRelationships(graphModel);
            return mappingContext.root();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mapNodes(GraphModel graphModel) throws Exception {
        for (NodeModel node : graphModel.getNodes()) {

            Object object = this.objectCreator.instantiateObjectMappedTo(node);

            mappingContext.register(object, node.getId());

            setProperties(node, object);
        }
    }

    private void mapRelationships(GraphModel graphModel) throws Exception {

        final List<EdgeModel> vectorRelationships = new ArrayList<>();

        for (EdgeModel edge : graphModel.getRelationships()) {
            Object parent = mappingContext.get(edge.getStartNode());
            Object child = mappingContext.get(edge.getEndNode());
            if (setValue(parent, child)) {
                mappingContext.evict(child.getClass());
            } else {
                vectorRelationships.add(edge);
            }
        }
        mapOneToMany(vectorRelationships);
    }

    private void mapOneToMany(List<EdgeModel> vectorRelationships) throws Exception {
        for (EdgeModel edge : vectorRelationships) {
            Object instance = mappingContext.get(edge.getStartNode());
            Object parameter = mappingContext.get(edge.getEndNode());
            Class<?> type = parameter.getClass();
            if (mappingContext.get(type) != null) {
                entityAccessStrategyFactory.forType(type).setIterable(instance, mappingContext.get(type));
                mappingContext.evict(type); // we've added all instances of type, no point in repeating the effort.
            }
        }
    }

    private void setProperties(NodeModel nodeModel, Object instance) throws Exception {
        for (Property property : nodeModel.getAttributes()) {
            // XXX this is still implicitly saying property.name => object.field
            PersistentField pf = mappingMetadata.lookUpPersistentFieldForProperty(property);
            entityAccessStrategyFactory.forPersistentField(pf).set(instance, property.getValue());
        }
    }

    private boolean setValue(Object instance, Object parameter) throws Exception {
        try {
            this.entityAccessStrategyFactory.forType(parameter.getClass()).setValue(instance, parameter);
            return true;
        } catch (NoSuchMethodException me) {
            return false;
        }
    }

}
