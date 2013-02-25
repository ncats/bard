package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods to manipulate JSON objects.
 *
 * @author Rajarshi Guha
 */
public class JsonUtil {

    public static JsonNode getJsonSchema(Class klass) throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();

        JsonSchema schema = mapper.generateJsonSchema(klass);
        JsonNode schemaRootNode = schema.getSchemaNode();

        // lets examine the class to see if any fields were marked
        // with @BARDJsonRequired and if so include a 'required' field
        // in the schema
        List<String> requiredFieldNames = new ArrayList<String>();
        for (Field field : klass.getDeclaredFields()) {
            if (field.isAnnotationPresent(BARDJsonRequired.class))
                requiredFieldNames.add(field.getName());

            // Hack to deal with Map<,> fields, where key and value are primitive types
            if (field.getType().equals(Map.class)) {
                JsonNode mapNode = getJsonSchemaForMap(field);
                ObjectNode propertiesNode = (ObjectNode) schemaRootNode.get("properties");
                propertiesNode.put(field.getName(), mapNode);
            }
        }
        if (requiredFieldNames.size() > 0) {
            ((ObjectNode) schemaRootNode).put("required", mapper.valueToTree(requiredFieldNames));
        }

        return schemaRootNode;
    }

    static String getJsonTypeFromJavaType(Class klass) {
        String jtype = "string";
        if (klass == null) jtype = "null";
        else if (klass.equals(Boolean.class)) jtype = "boolean";
        else if (klass.equals(Integer.class)) jtype = "integer";
        else if (klass.equals(BigInteger.class)) jtype = "integer";
        else if (klass.equals(Float.class)) jtype = "number";
        else if (klass.equals(Double.class)) jtype = "number";
        else if (klass.equals(Long.class)) jtype = "number";
        return jtype;
    }

    public static JsonNode getJsonSchemaForMap(Field field) {
        ObjectMapper mapper = new ObjectMapper();

        ParameterizedType mapType = (ParameterizedType) field.getGenericType();
        Type keyType = mapType.getActualTypeArguments()[0];
        Type valType = mapType.getActualTypeArguments()[1];

        String jsonKeyType = getJsonTypeFromJavaType(keyType.getClass());
        String jsonValType = getJsonTypeFromJavaType(valType.getClass());

        ObjectNode node = mapper.createObjectNode();
        node.put("type", "array");

        ObjectNode itemsNode = mapper.createObjectNode();
        itemsNode.put("type", "object");

        ObjectNode propsNode = mapper.createObjectNode();
        propsNode.put("key", jsonKeyType);
        propsNode.put("value", jsonValType);

        itemsNode.put("properties", propsNode);
        node.put("items", itemsNode);

        return node;
    }
}
