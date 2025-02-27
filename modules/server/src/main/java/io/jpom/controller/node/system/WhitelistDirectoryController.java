package io.jpom.controller.node.system;

import cn.hutool.core.util.ReflectUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 白名单目录
 *
 * @author jiangzeyin
 * @date 2019/2/28
 */
@RestController
@RequestMapping(value = "/node/system")
@Feature(cls = ClassFeature.NODE_CONFIG_WHITELIST)
@SystemPermission
public class WhitelistDirectoryController extends BaseServerController {

	private final WhitelistDirectoryService whitelistDirectoryService;

	public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
		this.whitelistDirectoryService = whitelistDirectoryService;
	}


	/**
	 * get whiteList data
	 * 白名单数据接口
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	public String whiteList() {
		AgentWhitelist agentWhitelist = whitelistDirectoryService.getData(getNode());
		Map<String, String> map = new HashMap<>(8);
		if (agentWhitelist != null) {
			/**
			 * put key and value into map
			 * 赋值给 map 对象返回
			 */
			Field[] fields = ReflectUtil.getFields(AgentWhitelist.class);
			for (Field field : fields) {
				Collection<String> fieldValue = (Collection<String>) ReflectUtil.getFieldValue(agentWhitelist, field);
				map.put(field.getName(), AgentWhitelist.convertToLine(fieldValue));
			}
		}
		return JsonMessage.getString(200, "ok", map);
	}


	/**
	 * 保存接口
	 *
	 * @return json
	 */
	@RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	public String whitelistDirectorySubmit() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.WhitelistDirectory_Submit).toString();
	}
}
