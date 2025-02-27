<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 表格 :scroll="{ x: 1070, y: tableHeight -60 }" scroll 跟 expandedRowRender 不兼容，没法同时使用不然会多出一行数据-->
    <a-table
      :columns="columns"
      :data-source="list"
      bordered
      rowKey="id"
      @expand="expand"
      :pagination="this.listQuery.total / this.listQuery.limit > 1 ? (this, pagination) : false"
      @change="changePage"
    >
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%id%']" placeholder="节点ID" />
          <a-input v-model="listQuery['%name%']" placeholder="节点名称" />
          <a-input v-model="listQuery['%url%']" placeholder="节点地址" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.group" allowClear placeholder="分组" class="search-input-item">
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary jpom-node-manage-add" @click="handleAdd">新增</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" @click="fastInstall">快速安装</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

          <a-tooltip>
            <template slot="title">
              <div>点击节点管理进入节点管理页面</div>

              <div>
                <ul>
                  <li>节点账号密码为插件端的账号密码,并非用户账号(管理员)密码</li>
                  <li>节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json 文件查看（如果自定义配置了账号密码将没有此文件）</li>
                  <li>节点地址为插件端的 IP:PORT 插件端端口默认为：2123</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip> </a-space
      ></template>
      <a-tooltip slot="url" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="name" slot-scope="text, record">
        <a-tooltip title="我在这里" :visible="showOptVisible[record.id]">
          <span>{{ text }}</span>
        </a-tooltip>
      </template>
      <a-tooltip slot="cycle" slot-scope="text" placement="topLeft" :title="nodeMonitorCycle[text]">
        <span>{{ nodeMonitorCycle[text] }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-tooltip title="我在这里" :visible="showOptVisible[record.id]">
          <a-space>
            <a-button v-if="record.unLockType" type="primary" @click="unlock(record)"><a-icon type="unlock" />解锁</a-button>

            <a-tooltip v-else title="如果按钮不可用则表示当前节点已经关闭啦,需要去编辑中启用">
              <a-button class="jpom-node-manage-btn" type="primary" @click="handleNode(record)" :disabled="record.openStatus !== 1"><a-icon type="apartment" />管理</a-button>
            </a-tooltip>
            <a-tooltip title="需要到编辑中去为一个节点绑定一个 ssh信息才能启用该功能">
              <a-button type="primary" @click="handleTerminal(record)" :disabled="!record.sshId"><a-icon type="code" />终端</a-button>
            </a-tooltip>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                更多
                <a-icon type="down" />
              </a>
              <a-menu slot="overlay">
                <a-menu-item>
                  <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
                </a-menu-item>

                <a-menu-item>
                  <a-tooltip title="删除会检查数据关联性,并且节点不存在项目或者脚本">
                    <a-button type="danger" @click="handleDelete(record)">删除</a-button>
                  </a-tooltip>
                </a-menu-item>
                <a-menu-item>
                  <a-tooltip title="解绑会检查数据关联性,同时将自动删除节点项目和脚本缓存信息,一般用于服务器无法连接且已经确定不再使用">
                    <a-button type="danger" @click="handleUnbind(record)">解绑</a-button>
                  </a-tooltip>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
          </a-space>
        </a-tooltip>
      </template>
      <!-- 嵌套表格 -->
      <a-table slot="expandedRowRender" slot-scope="text" :loading="childLoading" :columns="childColumns" :data-source="text.children" :pagination="false" :rowKey="(record, index) => text.id + index">
        <a-tooltip slot="osName" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="javaVersion" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="runTime" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="projectCount" slot-scope="text, item">
          <div v-if="text" @click="syncNode(item)">
            <a-tooltip placement="topLeft" title="节点中的所有项目数量,点击重新同步节点项目信息">
              <a-tag>{{ text }} </a-tag>
              <a-icon type="sync" />
            </a-tooltip>
          </div>
        </template>
        <template slot="scriptCount" slot-scope="text, item">
          <div v-if="text" @click="syncNodeScript(item)">
            <a-tooltip placement="topLeft" title="节点中的所有脚本模版数量,点击重新同步脚本模版信息">
              <a-tag>{{ text }} </a-tag>
              <a-icon type="sync" />
            </a-tooltip>
          </div>
        </template>
      </a-table>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editNodeVisible" title="编辑节点" @ok="handleEditNodeOk" :maskClosable="false">
      <a-form-model ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <!-- <a-form-model-item label="节点 ID" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改" />
        </a-form-model-item> -->
        <a-form-model-item label="节点名称" prop="name">
          <a-input v-model="temp.name" placeholder="节点名称" />
        </a-form-model-item>
        <a-form-model-item label="分组名称" prop="group">
          <custom-select v-model="temp.group" :data="groupList" suffixIcon="" inputPlaceholder="添加分组" selectPlaceholder=""> </custom-select>
        </a-form-model-item>
        <a-form-model-item label="绑定 SSH " prop="sshId">
          <a-select show-search option-filter-prop="children" v-model="temp.sshId" placeholder="请选择SSH">
            <a-select-option value="">不绑定</a-select-option>
            <a-select-option v-for="ssh in sshList" :key="ssh.id" :disabled="ssh.disabled">{{ ssh.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <!-- <a-form-model-item label="监控周期" prop="cycle">
          <a-select v-model="temp.cycle" defaultValue="0" placeholder="监控周期">
            <a-select-option v-for="(name, key) in nodeMonitorCycle" :key="parseInt(key)">{{ name }}</a-select-option>
          </a-select>
        </a-form-model-item> -->

        <a-form-model-item label="节点状态" prop="openStatus">
          <a-switch
            :checked="temp.openStatus == 1"
            @change="
              (checked) => {
                temp.openStatus = checked ? 1 : 0;
              }
            "
            checked-children="启用"
            un-checked-children="停用"
            default-checked
          />
        </a-form-model-item>
        <a-form-model-item prop="url">
          <template slot="label">
            节点地址
            <a-tooltip v-show="!temp.id">
              <template slot="title"
                >节点地址为插件端的 IP:PORT 插件端端口默认为：2123
                <ul>
                  <li>节点地址建议使用内网地址</li>
                  <li>如果插件端正常运行但是连接失败请检查端口是否开放,防火墙规则,云服务器的安全组入站规则</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)">
            <a-select slot="addonBefore" v-model="temp.protocol" default-value="Http://" style="width: 80px">
              <a-select-option value="Http"> Http:// </a-select-option>
              <a-select-option value="Https"> Https:// </a-select-option>
            </a-select>
            <!--						<a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)"/>-->
          </a-input>
        </a-form-model-item>
        <!--				<a-form-model-item label="节点协议" prop="protocol">-->
        <!--					<a-select v-model="temp.protocol" defaultValue="http" placeholder="节点协议">-->
        <!--						<a-select-option key="http">HTTP</a-select-option>-->
        <!--						<a-select-option key="htts">HTTPS</a-select-option>-->
        <!--					</a-select>-->
        <!--				</a-form-model-item>-->
        <!--				<a-form-model-item label="节点地址" prop="url">-->
        <!--					<a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)"/>-->
        <!--				</a-form-model-item>-->
        <div class="node-config">
          <a-form-model-item label="节点账号" prop="loginName">
            <a-input v-model="temp.loginName" placeholder="节点账号,请查看节点启动输出的信息" />
          </a-form-model-item>
          <a-form-model-item :prop="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
            <template slot="label">
              节点密码
              <a-tooltip v-show="!temp.id">
                <template slot="title"> 节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json 文件查看（如果自定义配置了账号密码将没有此文件） </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-password v-model="temp.loginPwd" placeholder="节点密码,请查看节点启动输出的信息" />
          </a-form-model-item>
        </div>
        <a-form-model-item label="超时时间(s)" prop="timeOut">
          <a-input-number v-model="temp.timeOut" :min="0" placeholder="秒 (值太小可能会取不到节点状态)" style="width: 100%" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 管理节点 -->
    <a-drawer :title="drawerTitle" placement="right" :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`" :visible="drawerVisible" @close="onClose">
      <!-- 节点管理组件 -->
      <node-layout v-if="drawerVisible" :node="temp" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal v-model="terminalVisible" width="80%" title="Terminal" :footer="null" :maskClosable="false">
      <terminal v-if="terminalVisible" :sshId="temp.sshId" :nodeId="temp.id" />
    </a-modal>
    <!-- 解锁节点 -->
    <a-modal v-model="unlockNode" title="解锁节点" @ok="handleUnLockNodeOk" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="绑定工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 快速安装插件端 -->
    <a-modal v-model="fastInstallNode" width="80%" title="快速安装插件端" :footer="null" :maskClosable="false" @cancel="cancelFastInstall">
      <div v-if="fastInstallInfo">
        <a-collapse v-model="fastInstallActiveKey">
          <a-collapse-panel key="1" header="温馨提示">
            <a-alert message="温馨提示" type="warning">
              <template slot="description">
                <ul>
                  <li>复制下面任意一条命令到还未安装插件端的服务器中去执行,执行前需要放行<b>防火墙端口</b>,<b>安全组规则</b>等网络端口限制</li>
                  <li>插件端运行端口默认使用：<b>2123</b></li>
                  <li>执行前需要检查命令中的地址在对应的服务器中是否可以访问,如果无法访问将不能自动绑定节点,<b>会使用 PING 检查</b></li>
                  <li>插件端安装并启动成功后将主动上报节点信息,如果上报的 IP+PROP 能正常通讯将添加节点信息</li>
                  <li>如果上报的节点信息包含多个 IP 地址需要用户确认使用具体的 IP 地址信息</li>
                  <li>添加的节点(插件端)将自动<b>绑定到当前工作空间</b>,如果需要在其他工作空间需要提前切换生成命令</li>
                  <li>下面命令将在<b>重启服务端后失效</b>,重启服务端需要重新获取</li>
                </ul>
              </template>
            </a-alert>
          </a-collapse-panel>
          <a-collapse-panel key="2" header="快速安装">
            <a-tabs :default-active-key="0">
              <a-tab-pane v-for="(item, index) in fastInstallInfo.shUrls" :tab="item.name" :key="index">
                <div
                  v-clipboard:copy="item.allText"
                  v-clipboard:success="
                    () => {
                      tempVue.prototype.$notification.success({
                        message: '复制成功',
                      });
                    }
                  "
                  v-clipboard:error="
                    () => {
                      tempVue.prototype.$notification.error({
                        message: '复制失败',
                      });
                    }
                  "
                >
                  <a-alert type="info" :message="`命令内容(点击可以复制)`">
                    <template slot="description">
                      <span>{{ item.allText }} </span>
                      <a-icon type="copy" />
                    </template>
                  </a-alert>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-collapse-panel>
          <a-collapse-panel key="3" header="快速绑定">
            <a-alert
              v-clipboard:copy="fastInstallInfo.bindCommand"
              v-clipboard:success="
                () => {
                  tempVue.prototype.$notification.success({
                    message: '复制成功',
                  });
                }
              "
              v-clipboard:error="
                () => {
                  tempVue.prototype.$notification.error({
                    message: '复制失败',
                  });
                }
              "
              type="info"
              :message="`命令内容(点击可以复制、命令路径请修改为您的服务器中的实际路径)`"
            >
              <template slot="description">
                <span>{{ fastInstallInfo.bindCommand }} </span>
                <a-icon type="copy" />
              </template>
            </a-alert>
          </a-collapse-panel>
          <a-collapse-panel key="4" header="执行结果">
            <div v-if="!pullFastInstallResultData || !pullFastInstallResultData.length">还没有任何结果</div>
            <a-alert
              :message="`第 ${index + 1} 个结果`"
              :type="`${item.type === 'success' ? 'success' : item.type === 'exists' ? 'error' : 'warning'}`"
              v-for="(item, index) in pullFastInstallResultData"
              :key="`${index}-${new Date().getTime()}`"
              closable
              @close="clearPullFastInstallResult(item.id)"
            >
              <template slot="description">
                <a-space direction="vertical">
                  <div v-if="item.type === 'canUseIpEmpty'"><a-tag color="orange">不能和节点正常通讯</a-tag></div>
                  <div v-if="item.type === 'multiIp'"><a-tag color="green">多IP可以使用</a-tag></div>
                  <div v-if="item.type === 'exists'"><a-tag color="orange">节点已经存在</a-tag></div>
                  <div v-if="item.type === 'success'"><a-tag color="orange">绑定成功</a-tag></div>
                  <div>
                    所有的IP：<a-tag v-for="(itemIp, indexIp) in item.allIp" :key="indexIp">{{ itemIp }}:{{ item.port }}</a-tag>
                  </div>
                  <div v-if="item.type === 'multiIp'">
                    能通讯的IP:
                    <a-tag @click="confirmFastInstall(item.id, itemIp, item.port)" v-for="(itemIp, indexIp) in item.canUseIp" :key="indexIp">{{ itemIp }}:{{ item.port }}<a-icon type="api" /></a-tag>
                  </div>
                  <div v-if="item.type === 'success' || item.type === 'exists'">
                    节点的IP: <a-tag v-for="(itemIp, indexIp) in item.canUseIp" :key="indexIp">{{ itemIp }}:{{ item.port }}</a-tag>
                  </div>
                </a-space>
              </template>
            </a-alert>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div v-else>loading....</div>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { getNodeList, getNodeStatus, editNode, unbind, deleteNode, syncProject, unLockWorkspace, getNodeGroupAll, fastInstall, pullFastInstallResult, confirmFastInstall } from "@/api/node";
import { getSshListAll } from "@/api/ssh";
import { syncScript } from "@/api/node-other";
import NodeLayout from "./node-layout";
import Terminal from "@/pages/ssh/terminal";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
import { getWorkSpaceListAll } from "@/api/workspace";
import CustomSelect from "@/components/customSelect";
import Vue from "vue";

export default {
  components: {
    NodeLayout,
    Terminal,
    CustomSelect,
  },
  data() {
    return {
      loading: false,
      childLoading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      // nodeMonitorCycle: nodeMonitorCycle,
      sshList: [],
      list: [],
      groupList: [],
      showOptVisible: {},
      temp: {
        timeOut: 0,
      },
      fastInstallActiveKey: ["1", "2", "4"],
      fastInstallInfo: null,
      tempVue: null,
      pullFastInstallResultTime: null,
      pullFastInstallResultData: [],
      editNodeVisible: false,
      drawerVisible: false,
      terminalVisible: false,
      unlockNode: false,
      fastInstallNode: false,
      drawerTitle: "",
      columns: [
        // { title: "节点 ID", dataIndex: "id", sorter: true, key: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "节点名称", dataIndex: "name", sorter: true, key: "name", ellipsis: true, scopedSlots: { customRender: "name" } },

        { title: "节点协议", dataIndex: "protocol", sorter: true, key: "protocol", width: 100, ellipsis: true, scopedSlots: { customRender: "protocol" } },
        { title: "节点地址", dataIndex: "url", sorter: true, key: "url", ellipsis: true, scopedSlots: { customRender: "url" } },
        { title: "账号", dataIndex: "loginName", sorter: true, key: "loginName", ellipsis: true, scopedSlots: { customRender: "loginName" } },
        // { title: "监控周期", dataIndex: "cycle", sorter: true, key: "cycle", ellipsis: true, scopedSlots: { customRender: "cycle" } },
        { title: "超时时间", dataIndex: "timeOut", sorter: true, key: "timeOut", width: 100, ellipsis: true },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", key: "operation", width: 270, scopedSlots: { customRender: "operation" }, align: "left" },
      ],
      childColumns: [
        { title: "系统名", dataIndex: "osName", key: "osName", width: 100, ellipsis: true, scopedSlots: { customRender: "osName" } },
        { title: "JDK 版本", dataIndex: "javaVersion", key: "javaVersion", ellipsis: true, scopedSlots: { customRender: "javaVersion" } },
        { title: "JVM 总内存", dataIndex: "totalMemory", key: "totalMemory", width: 120 },
        { title: "JVM 剩余内存", dataIndex: "freeMemory", key: "freeMemory", width: 140 },
        { title: "版本", dataIndex: "jpomVersion", key: "jpomVersion", width: 120 },
        { title: "Java 程序数", dataIndex: "javaVirtualCount", key: "javaVirtualCount", width: 120 },

        { title: "项目数", dataIndex: "count", key: "count", width: 90, scopedSlots: { customRender: "projectCount" } },
        { title: "脚本数", dataIndex: "scriptCount", key: "scriptCount", width: 90, scopedSlots: { customRender: "scriptCount" } },
        { title: "响应时间", dataIndex: "timeOut", key: "timeOut", width: 120 },
        { title: "已运行时间", dataIndex: "runTime", key: "runTime", width: 150, ellipsis: true, scopedSlots: { customRender: "runTime" } },
      ],
      rules: {
        id: [{ required: true, message: "Please input node id", trigger: "blur" }],
        name: [{ required: true, message: "Please input node name", trigger: "blur" }],
        url: [{ required: true, message: "Please input url", trigger: "blur" }],
        loginName: [{ required: true, message: "Please input login name", trigger: "blur" }],
        loginPwd: [{ required: true, message: "Please input login password", trigger: "blur" }],
        timeOut: [{ required: true, message: "Please input timeout", trigger: "blur" }],
      },
      workspaceList: [],
    };
  },
  computed: {
    ...mapGetters(["getCollapsed", "getWorkspaceId"]),
    pagination() {
      return {
        total: this.listQuery.total || 0,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  watch: {
    $route() {
      if (this.$route.query.tipNodeId) {
        this.showOptVisible[this.$route.query.tipNodeId] = true;
        this.showOptVisible = { ...this.showOptVisible };
        setTimeout(() => {
          this.showOptVisible[this.$route.query.tipNodeId] = false;
          this.showOptVisible = { ...this.showOptVisible };
          let query = Object.assign({}, this.$route.query);
          delete query.tipNodeId;
          this.$router.replace({
            query: query,
          });
        }, 10000);
      }
    },
  },
  created() {
    this.loadData();
    this.loadGroupList();
  },
  destroyed() {
    if (this.pullFastInstallResultTime) {
      clearInterval(this.pullFastInstallResultTime);
    }
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "node-edit",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".node-config"),
              intro: "节点的账号密码可以通过 agent_authorize.json 文件查看",
            },
          ],
        },
      });
    },
    introGuideList() {
      this.$store.dispatch("tryOpenGuide", {
        key: "node-list-manage",
        beforeKey: "index",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-manage-add"),
              intro: "如果还没有节点 可以点击【新增】按钮新增节点",
            },
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-manage-btn"),
              intro: "点击【节点管理】按钮可以进入节点管理,节点管理里面可以挖掘更多功能",
            },
          ],
        },
      });
    },
    // 获取所有的分组
    loadGroupList() {
      getNodeGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    // 加载 SSH 列表
    loadSshList() {
      getSshListAll().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.list = [];
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getNodeList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
          let nodeId = this.$route.query.nodeId;
          this.list.map((item) => {
            if (nodeId === item.id) {
              this.handleNode(item);
            }
          });
          this.$nextTick(() => {
            this.introGuideList();
          });
        }
        this.loading = false;
      });
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        if (!record.openStatus) {
          this.$notification.error({
            message: "节点未启用",
          });
          return false;
        }
        // 请求节点状态数据
        this.childLoading = true;
        getNodeStatus(record.id).then((res) => {
          if (res.code === 200) {
            // const index = this.list.findIndex(ele => ele.id === record.id);
            // this.list[index].children = res.data;
            record.children = res.data;
          }
          this.childLoading = false;
        });
      }
    },
    // 添加
    handleAdd() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
        this.$refs["editNodeForm"] && this.$refs["editNodeForm"].resetFields();
        this.temp = {
          type: "add",
          cycle: 0,
          protocol: "http",
          openStatus: 1,
          timeOut: 0,
          loginName: "jpomAgent",
        };
        this.editNodeVisible = true;
      });
      this.loadSshList();
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign(record);
      this.terminalVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.loadSshList();
      // this.temp.tempGroup = "";
      this.editNodeVisible = true;
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs["editNodeForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editNode(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editNodeForm"].resetFields();
            this.editNodeVisible = false;
            this.loadData();
            this.loadGroupList();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除节点么？删除会检查数据关联性,并且节点不存在项目或者脚本",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteNode(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 解绑
    handleUnbind(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要解绑节点么？解绑会检查数据关联性,同时将自动删除节点项目和脚本缓存信息,一般用于服务器无法连接且已经确定不再使用",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          unbind(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 管理节点
    handleNode(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `${this.temp.name} (${this.temp.url})`;
      this.drawerVisible = true;
      let nodeId = this.$route.query.nodeId;
      if (nodeId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, nodeId: record.id },
        });
      }
    },
    syncNode(node) {
      syncProject(node.nodeId).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
          return false;
        }
      });
    },
    syncNodeScript(node) {
      syncScript({
        nodeId: node.nodeId,
      }).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false;
      let query = Object.assign({}, this.$route.query);
      delete query.nodeId, delete query.id, delete query.pId;
      this.$router.replace({
        query: query,
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data;
        }
      });
    },

    unlock(record) {
      this.unlockNode = true;
      this.loadWorkSpaceListAll();

      this.temp = Object.assign({}, record);
      this.temp.workspaceId = "";
    },
    handleUnLockNodeOk() {
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: "请选择工作空间",
        });
        return false;
      }
      this.$confirm({
        title: "系统提示",
        content: "确定要将此节点绑定到这个工作空间吗？绑定后不可更改",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          unLockWorkspace({
            id: this.temp.id,
            workspaceId: this.temp.workspaceId,
          }).then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.unlockNode = false;
              this.loadData();
              return false;
            }
          });
        },
      });
    },
    // 关闭弹窗,关闭定时轮询
    cancelFastInstall() {
      if (this.pullFastInstallResultTime) {
        clearInterval(this.pullFastInstallResultTime);
      }
      this.loadData();
    },
    // 快速安装弹窗
    fastInstall() {
      fastInstall().then((res) => {
        if (res.code === 200) {
          this.fastInstallNode = true;
          this.fastInstallInfo = res.data;
          this.tempVue = Vue;
          this.fastInstallInfo.host = `${location.protocol}//${location.host}${res.data.url}?token=${res.data.token}&workspaceId=${this.getWorkspaceId}`;
          this.fastInstallInfo.shUrls = res.data.shUrls.map((item) => {
            item.allText = `${item.url} ${this.fastInstallInfo.key} '${this.fastInstallInfo.host}'`;
            return item;
          });
          this.fastInstallInfo.bindCommand = `sh /xxxx/Agent.sh restart ${this.fastInstallInfo.key} '${this.fastInstallInfo.host}'`;
          // 轮询 结果
          this.pullFastInstallResultTime = setInterval(() => {
            pullFastInstallResult().then((res) => {
              if (res.code === 200) {
                this.pullFastInstallResultData = res.data;
              }
            });
          }, 2000);
        }
      });
    },
    // 清除快速安装节点缓存
    clearPullFastInstallResult(id) {
      pullFastInstallResult({
        removeId: id,
      }).then((res) => {
        if (res.code === 200) {
          this.pullFastInstallResultData = res.data;
        }
      });
    },
    // 安装确认
    confirmFastInstall(id, ip, port) {
      confirmFastInstall({
        id: id,
        ip: ip,
        port: port,
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.pullFastInstallResultData = res.data;
        }
      });
    },
  },
};
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
} */
/* 
.filter-item {
  width: 150px;
  margin-right: 10px;
} */
/* 
.btn-add {
  margin-left: 10px;
  margin-right: 0;
} */
</style>
