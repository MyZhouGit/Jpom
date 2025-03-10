<template>
  <a-layout class="log-layout node-full-content">
    <!-- 侧边栏 文件树 -->
    <a-layout-sider theme="light" class="log-sider jpom-node-log-tree" width="20%">
      <a-empty v-if="list.length === 0" />
      <a-directory-tree :treeData="list" :replaceFields="replaceFields" @select="select" default-expand-all> </a-directory-tree>
    </a-layout-sider>
    <!-- 单个文件内容 -->
    <a-layout-content class="log-content">
      <!-- <div class="filter">
        <a-button type="primary" @click="loadData">刷新</a-button>
      </div>
      <div>
        <a-input class="console" v-model="logContext" readOnly type="textarea" style="resize: none" />
      </div> -->
      <log-view :ref="`logView`" height="calc(100vh - 165px)" :extendBar="false">
        <template slot="before">
          <a-space>
            <a-button type="primary" size="small" @click="loadData">刷新</a-button>
            <a-button type="danger" size="small" :disabled="!this.temp.path" @click="deleteLog">删除</a-button>
            <a-button type="primary" size="small" :disabled="!this.temp.path" @click="downloadLog">下载</a-button>
          </a-space>
        </template>
      </log-view>
    </a-layout-content>
    <!-- 对话框 -->
    <!-- <a-modal v-model="visible" title="系统提示" :footer="null">
      <a-space>
        <a-button type="danger" @click="deleteLog">删除日志文件</a-button>
        <a-button type="primary" @click="downloadLog">下载日志文件</a-button>
        <a-button @click="visible = false">取消</a-button>
      </a-space>
    </a-modal> -->
  </a-layout>
</template>
<script>
import { getLogList, downloadFile, deleteLog } from "@/api/system";
import { mapGetters } from "vuex";
import { getWebSocketUrl } from "@/utils/const";
import LogView from "@/components/logView";
export default {
  components: {
    LogView,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      list: [],
      socket: null,
      // 日志内容
      logContext: "choose file loading context...",
      tomcatId: "system",
      replaceFields: {
        children: "children",
        title: "title",
        key: "path",
      },
      // visible: false,
      temp: {},
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      return getWebSocketUrl("/socket/tomcat_log", `userId=${this.getLongTermToken}&tomcatId=${this.tomcatId}&nodeId=${this.node.id}&type=tomcat`);
    },
  },
  watch: {},
  created() {
    this.loadData();
    this.$nextTick(() => {
      setTimeout(() => {
        this.introGuide();
      }, 500);
    });
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "node-log",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-log-tree"),
              intro: "这里是 Agent 节点里面的日志文件，点击具体的文件可以在右边的区域查看日志内容。",
            },
            {
              title: "导航助手",
              element: document.querySelector(".ant-tree-node-content-wrapper"),
              intro: "您还可以用右键点击，会弹出一个操作选项的窗口（嗯，入口隐藏的比较深，所以有必要提示一下）。",
            },
          ],
        },
      });
    },
    // 加载数据
    loadData() {
      this.list = [];
      const params = { nodeId: this.node.id };
      getLogList(params).then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            if (element.children) {
              this.calcTreeNode(element.children);
            }
            // 组装数据
            this.list.push({
              ...element,
              isLeaf: !element.children ? true : false,
            });
          });
        }
      });
    },
    // 递归处理节点
    calcTreeNode(list) {
      list.forEach((element) => {
        if (element.children) {
          this.calcTreeNode(element.children);
        } else {
          // 叶子节点
          element.isLeaf = true;
        }
      });
    },
    // 选择节点
    select(selectedKeys, { node }) {
      if (this.temp?.path === node.dataRef?.path) {
        return;
      }
      if (!node.dataRef.isLeaf) {
        return;
      }
      const data = {
        op: "showlog",
        tomcatId: this.tomcatId,
        fileName: node.dataRef.path,
      };
      this.temp = node.dataRef;
      this.$refs.logView.clearLogCache();

      this.socket?.close();

      this.socket = new WebSocket(this.socketUrl);
      // 连接成功后
      this.socket.onopen = () => {
        this.socket.send(JSON.stringify(data));
      };
      this.socket.onmessage = (msg) => {
        this.$refs.logView.appendLine(msg.data);
      };
      this.socket.onerror = (err) => {
        console.error(err);
        this.$notification.error({
          message: "web socket 错误,请检查是否开启 ws 代理",
        });
      };
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err);
        this.$message.warning("会话已经关闭");
        // clearInterval(this.heart);
      };
    },
    // // 右键点击
    // rightClick({ node }) {
    //   this.temp = node.dataRef;
    //   // 弹出提示 下载还是删除
    //   this.visible = true;
    // },
    // 下载文件
    downloadLog() {
      // 请求参数
      const params = {
        nodeId: this.node.id,
        path: this.temp.path,
      };
      // 请求接口拿到 blob
      window.open(downloadFile(params), "_self");
    },
    // 删除文件
    deleteLog() {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除日志文件么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          const params = {
            nodeId: this.node.id,
            path: this.temp.path,
          };
          // 删除日志
          deleteLog(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.visible = false;
              this.loadData();
            }
          });
        },
      });
    },
  },
};
</script>
<style scoped>
.log-layout {
  padding: 0;
  margin: 0;
}
.log-sider {
  border: 1px solid #e2e2e2;
  /* height: calc(100vh - 130px); */
  overflow-x: auto;
}
.log-content {
  margin: 0;
  padding-left: 15px;
  background: #fff;
  height: calc(100vh - 130px);
  overflow-y: auto;
}
/* .console {
  padding: 5px;
  color: #fff;
  font-size: 14px;
  background-color: black;
  width: 100%;
  height: calc(100vh - 185px);
  overflow-y: auto;
  border: 1px solid #e2e2e2;
  border-radius: 5px 5px;
} */
</style>
