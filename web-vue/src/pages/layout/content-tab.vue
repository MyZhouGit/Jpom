<template>
  <a-tabs v-model="activeKey" class="my-tabs" hide-add type="editable-card" @edit="onEdit" @change="changeTab">
    <a-tab-pane v-for="(tab, index) in nowTabList" :key="tab.key" :closable="nowTabList.length > 1">
      <template slot="tab">
        <a-dropdown :trigger="['contextmenu']">
          <span style="display: inline-table">{{ tab.title }}</span>
          <a-menu slot="overlay">
            <a-menu-item
              @click="
                closeTabs({
                  key: tab.key,
                })
              "
            >
              <a-button type="link" :disabled="nowTabList.length <= 1">关闭其他</a-button>
            </a-menu-item>
            <a-menu-item
              @click="
                closeTabs({
                  key: tab.key,
                  position: 'left',
                })
              "
            >
              <a-button type="link" :disabled="nowTabList.length <= 1 || index === 0">关闭左侧</a-button>
            </a-menu-item>
            <a-menu-item
              @click="
                closeTabs({
                  key: tab.key,
                  position: 'right',
                })
              "
            >
              <a-button type="link" :disabled="nowTabList.length <= 1 || index === nowTabList.length - 1">关闭右侧</a-button>
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </template>
    </a-tab-pane>
    <template slot="tabBarExtraContent"> <user-header :mode="this.mode" /> </template>
  </a-tabs>
</template>
<script>
import { mapGetters } from "vuex";
import UserHeader from "./user-header";
export default {
  props: {
    mode: {
      type: String,
    },
  },
  components: {
    UserHeader,
  },
  data() {
    return {};
  },
  computed: {
    ...mapGetters(["getActiveTabKey", "getManagementActiveTabKey", "getTabList", "getManagementTabList", "getCollapsed"]),
    activeKey: {
      get() {
        return this.mode === "normal" ? this.getActiveTabKey : this.getManagementActiveTabKey;
      },
      set(value) {
        this.activeTab(value);
      },
    },
    nowTabList() {
      return this.mode === "normal" ? this.getTabList : this.getManagementTabList;
    },
  },
  created() {},
  methods: {
    // 编辑 Tab
    onEdit(key, action) {
      if (action === "remove") {
        if (this.nowTabList.length === 1) {
          this.$notification.warn({
            message: "不能关闭了",
          });
          return;
        }
        this.$store.dispatch(this.mode === "normal" ? "removeTab" : "removeManagementTab", key).then(() => {
          this.activeTab();
        });
      }
    },
    // 改变 Tab
    changeTab() {},
    // 关闭 tabs
    closeTabs(data) {
      this.$notification.success({
        message: "操作成功",
      });
      this.$store.dispatch(this.mode === "normal" ? "clearTabs" : "clearManagementTabs", data).then(() => {
        this.activeTab();
      });
    },
    activeTab(key) {
      key = key || this.activeKey;
      const index = this.nowTabList.findIndex((ele) => ele.key === key);
      const activeTab = this.nowTabList[index];
      this.$router.push({ query: { ...this.$route.query, sPid: activeTab.parentId, sId: activeTab.id }, path: activeTab.path });
      //
      this.$store.dispatch(this.mode === "normal" ? "activeMenu" : "activeManagementMenu", activeTab.id);
      this.$store.dispatch(this.mode === "normal" ? "menuOpenKeys" : "menuManagementOpenKeys", activeTab.parentId);
      return activeTab;
    },
  },
};
</script>
<style scoped>
.my-tabs {
  flex: auto;
  /* margin-right: 20px; */
  align-self: center;
  height: 40px;
}
</style>
