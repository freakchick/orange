package com.jq.orange.engine;

import com.jq.orange.SqlMeta;
import com.jq.orange.context.Context;
import com.jq.orange.node.SqlNode;
import com.jq.orange.tag.XmlParser;
import com.jq.orange.token.TokenHandler;
import com.jq.orange.token.TokenParser;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态sql引擎
 *
 * @program: orange
 * @description:
 * @author: jiangqiang
 * @create: 2021-02-22 18:21
 **/
public class DynamicSqlEngine {

    Cache cache = new Cache();

    public SqlMeta parse(String text, Map<String, Object> params) {
        text = String.format("<root>%s</root>", text);
        SqlNode sqlNode = parseXml2SqlNode(text);
        Context context = new Context(params);
        parseSqlText(sqlNode, context);
        parseParameter(context);
        SqlMeta sqlMeta = new SqlMeta(context.getSql(), context.getJdbcParameters());
        return sqlMeta;
    }

    public SqlNode parseXml2SqlNode(String text) {
        SqlNode node = cache.getNodeCache().get(text);
        if (node == null) {
            node = XmlParser.parseXml2SqlNode(text);
            cache.getNodeCache().put(text, node);
        }
        return node;
    }

    /**
     * 解析标签，去除标签，替换 ${}为常量值, #{}保留不变
     *
     * @param sqlNode
     * @param context
     */
    public void parseSqlText(SqlNode sqlNode, Context context) {
        sqlNode.apply(context);
    }

    /**
     * #{}替换成?，并且将?对应的参数值按顺序保存起来
     *
     * @param context
     */
    public void parseParameter(Context context) {
        TokenParser tokenParser = new TokenParser("#{", "}", new TokenHandler() {
            @Override
            public String handleToken(String content) {
                Object value = context.getOgnlValue(content);
                if (value == null) {
                    throw new RuntimeException("could not found value : " + content);
                }
                context.addParameter(value);
                return "?";
            }
        });
        String sql = tokenParser.parse(context.getSql());
        context.setSql(sql);
    }

    public static void main(String[] args) {
        DynamicSqlEngine engine = new DynamicSqlEngine();
        String sql = ("<root>select <if test='minId != null'>id > ${minId} #{minId} <if test='maxId != null'> and id &lt; ${maxId} #{maxId}</if> </if></root>");
        Map<String, Object> map = new HashMap<>();
        map.put("minId", 100);
        map.put("maxId", 500);
        engine.parse(sql, map);
    }
}
